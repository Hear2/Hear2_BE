package com.hear2.whatif.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.coupledna.dto.CoupleDnaResponse;
import com.hear2.coupledna.service.CoupleDnaService;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiTag;
import com.hear2.memory.entity.MemoryPhotoMetadata;
import com.hear2.memory.repository.MemoryRepository;
import com.hear2.qna.entity.DailyAnswer;
import com.hear2.qna.entity.DailyQuestion;
import com.hear2.qna.entity.DailyQuestionTemplate;
import com.hear2.qna.repository.DailyAnswerRepository;
import com.hear2.qna.repository.DailyQuestionRepository;
import com.hear2.qna.repository.DailyQuestionTemplateRepository;
import com.hear2.whatif.dto.WhatIfUsedDataSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WhatIfContextCollector {

    private static final int EMOTION_WINDOW_DAYS = 30;
    private static final int JUDGE_WINDOW_DAYS = 90;
    private static final int MEMORY_WINDOW_DAYS = 90;
    private static final int RECENT_MESSAGE_LIMIT = 12;
    private static final int MESSAGE_MAX_LENGTH = 200;
    private static final int RECENT_QNA_LIMIT = 10;

    private final ChatParticipantResolver chatParticipantResolver;
    private final CoupleRepository coupleRepository;
    private final CoupleDnaService coupleDnaService;
    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final DailyQuestionTemplateRepository dailyQuestionTemplateRepository;
    private final MemoryRepository memoryRepository;

    @Transactional(readOnly = true)
    public WhatIfContext collect(Long requesterId) {
        ChatParticipantResolver.ChatRoomContext roomContext = chatParticipantResolver.resolve(requesterId);
        Long coupleId = roomContext.coupleId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Couple couple = coupleRepository.findById(coupleId).orElse(null);
        CoupleDnaResponse coupleDna = loadCoupleDna(coupleId, today);
        List<ChatMessage> recentMessages = loadRecentMessages(coupleId, now);
        List<ChatMessage> textMessages = recentMessages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .filter(message -> StringUtils.hasText(message.getContent()))
                .toList();
        Map<Long, EmotionAnalysis> analysesByMessageId = loadAnalyses(textMessages);
        List<JudgeHistory> judgeHistories = loadRecentJudgeHistories(coupleId, now);
        QnaSummary qnaSummary = loadQnaSummary(coupleId, roomContext.senderId(), roomContext.receiverId());
        MemorySummary memorySummary = loadMemorySummary(coupleId, today);
        List<Map<String, Object>> limitedMessages = limitedRecentMessages(textMessages, analysesByMessageId);

        WhatIfUsedDataSummaryResponse usedDataSummary = WhatIfUsedDataSummaryResponse.builder()
                .coupleDnaUsed(coupleDna != null)
                .emotionDays(EMOTION_WINDOW_DAYS)
                .chatMessageCount(textMessages.size())
                .limitedRecentMessageCount(limitedMessages.size())
                .judgeHistoryCount(judgeHistories.size())
                .qnaAnswerCount(qnaSummary.answerCount())
                .memoryCount(memorySummary.memoryCount())
                .build();

        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("coupleProfile", buildCoupleProfile(couple, coupleId, requesterId, roomContext.receiverId(), today));
        promptData.put("coupleDna", summarizeCoupleDna(coupleDna));
        promptData.put("recentEmotionSummary", summarizeEmotion(textMessages, analysesByMessageId));
        promptData.put("conflictSummary", summarizeJudgeHistories(judgeHistories));
        promptData.put("qnaSummary", qnaSummary.promptData());
        promptData.put("memorySummary", memorySummary.promptData());
        promptData.put("limitedRecentMessages", limitedMessages);
        promptData.put("usedDataSummary", usedDataSummary);

        return new WhatIfContext(coupleId, requesterId, promptData, usedDataSummary);
    }

    private CoupleDnaResponse loadCoupleDna(Long coupleId, LocalDate today) {
        try {
            return coupleDnaService.getCoupleDna(coupleId, today);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<ChatMessage> loadRecentMessages(Long coupleId, LocalDateTime now) {
        return chatMessageRepository.findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                coupleId,
                now.minusDays(EMOTION_WINDOW_DAYS),
                now.plusSeconds(1)
        );
    }

    private Map<Long, EmotionAnalysis> loadAnalyses(List<ChatMessage> textMessages) {
        List<Long> messageIds = textMessages.stream()
                .map(ChatMessage::getId)
                .filter(Objects::nonNull)
                .toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        return emotionAnalysisRepository.findByMessageIdIn(messageIds).stream()
                .collect(Collectors.toMap(
                        analysis -> analysis.getMessage().getId(),
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private List<JudgeHistory> loadRecentJudgeHistories(Long coupleId, LocalDateTime now) {
        LocalDateTime startAt = now.minusDays(JUDGE_WINDOW_DAYS);
        return judgeHistoryRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId).stream()
                .filter(history -> history.getCreatedAt() == null || !history.getCreatedAt().isBefore(startAt))
                .limit(5)
                .toList();
    }

    private Map<String, Object> buildCoupleProfile(
            Couple couple,
            Long coupleId,
            Long requesterId,
            Long partnerId,
            LocalDate today
    ) {
        LocalDate startDate = couple == null ? null : couple.getStartDate();
        if (startDate == null && couple != null && couple.getCreatedAt() != null) {
            startDate = couple.getCreatedAt().toLocalDate();
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("coupleId", coupleId);
        profile.put("requesterId", requesterId);
        profile.put("partnerId", partnerId);
        profile.put("daysTogether", startDate == null ? null : ChronoUnit.DAYS.between(startDate, today) + 1);
        return profile;
    }

    private Map<String, Object> summarizeCoupleDna(CoupleDnaResponse coupleDna) {
        if (coupleDna == null) {
            return Map.of("available", false);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("available", true);
        summary.put("dnaTitle", coupleDna.getDnaTitle());
        summary.put("dnaDescription", coupleDna.getDnaDescription());
        summary.put("userAType", coupleDna.getUserAType());
        summary.put("userBType", coupleDna.getUserBType());
        summary.put("strengths", coupleDna.getStrengths());
        summary.put("emotionScore", coupleDna.getEmotionScore());
        summary.put("empathyScore", coupleDna.getEmpathyScore());
        summary.put("humorScore", coupleDna.getHumorScore());
        summary.put("recoveryScore", coupleDna.getRecoveryScore());
        summary.put("planningScore", coupleDna.getPlanningScore());
        summary.put("analyzedDays", coupleDna.getAnalyzedDays());
        return summary;
    }

    private Map<String, Object> summarizeEmotion(
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysesByMessageId
    ) {
        List<EmotionAnalysis> analyses = textMessages.stream()
                .map(message -> analysesByMessageId.get(message.getId()))
                .filter(Objects::nonNull)
                .toList();
        long positiveCount = analyses.stream()
                .filter(analysis -> analysis.getEmotionType() != null && !analysis.getEmotionType().isNegative())
                .count();
        double negativeScoreAverage = analyses.stream()
                .map(EmotionAnalysis::getNegativeScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        Map<String, Long> emotionCounts = analyses.stream()
                .filter(analysis -> analysis.getEmotionType() != null)
                .collect(Collectors.groupingBy(
                        analysis -> analysis.getEmotionType().name(),
                        Collectors.counting()
                ));
        Map<String, Long> riskCounts = analyses.stream()
                .filter(analysis -> analysis.getRiskLevel() != null && analysis.getRiskLevel() != RiskLevel.NONE)
                .collect(Collectors.groupingBy(
                        analysis -> analysis.getRiskLevel().name(),
                        Collectors.counting()
                ));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("periodDays", EMOTION_WINDOW_DAYS);
        summary.put("totalTextMessages", textMessages.size());
        summary.put("analyzedTextMessages", analyses.size());
        summary.put("positiveRatio", analyses.isEmpty() ? 0 : Math.round(positiveCount * 100.0 / analyses.size()));
        summary.put("negativeScoreAverage", Math.round(negativeScoreAverage * 100.0) / 100.0);
        summary.put("emotionCounts", emotionCounts);
        summary.put("riskCounts", riskCounts);
        return summary;
    }

    private Map<String, Object> summarizeJudgeHistories(List<JudgeHistory> judgeHistories) {
        Map<String, Long> conflictTypes = judgeHistories.stream()
                .filter(history -> history.getConflictType() != null)
                .collect(Collectors.groupingBy(
                        history -> history.getConflictType().name(),
                        Collectors.counting()
                ));
        List<Map<String, Object>> latest = judgeHistories.stream()
                .limit(3)
                .map(history -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("conflictType", history.getConflictType() == null ? null : history.getConflictType().name());
                    item.put("triggerRiskLevel", history.getTriggerRiskLevel() == null ? null : history.getTriggerRiskLevel().name());
                    item.put("judgement", truncate(history.getJudgement(), 180));
                    item.put("solution", truncate(history.getSolution(), 180));
                    item.put("createdAt", history.getCreatedAt());
                    return item;
                })
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("recentConflictCount", judgeHistories.size());
        summary.put("topConflictTypes", conflictTypes);
        summary.put("latestHistories", latest);
        return summary;
    }

    private QnaSummary loadQnaSummary(Long coupleId, Long requesterId, Long partnerId) {
        List<DailyQuestion> questions = dailyQuestionRepository.findByCoupleIdOrderByQuestionDateDescQuestionIdDesc(coupleId)
                .stream()
                .limit(RECENT_QNA_LIMIT)
                .toList();
        if (questions.isEmpty()) {
            return new QnaSummary(0, Map.of("recentAnsweredCount", 0, "notableAnswers", List.of()));
        }

        List<Long> questionIds = questions.stream().map(DailyQuestion::getQuestionId).toList();
        Map<Long, String> questionTexts = loadQuestionTexts(questions);
        List<DailyAnswer> answers = dailyAnswerRepository.findByQuestionIdIn(questionIds);
        Map<Long, List<DailyAnswer>> answersByQuestionId = answers.stream()
                .collect(Collectors.groupingBy(DailyAnswer::getQuestionId));

        List<Map<String, Object>> notableAnswers = questions.stream()
                .map(question -> {
                    List<DailyAnswer> questionAnswers = answersByQuestionId.getOrDefault(question.getQuestionId(), List.of());
                    if (questionAnswers.isEmpty()) {
                        return null;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("question", truncate(questionTexts.get(question.getTemplateId()), 120));
                    item.put("questionDate", question.getQuestionDate());
                    item.put("answers", questionAnswers.stream()
                            .sorted(Comparator.comparing(DailyAnswer::getUserId))
                            .map(answer -> {
                                Map<String, Object> answerItem = new LinkedHashMap<>();
                                answerItem.put("role", answer.getUserId().equals(requesterId)
                                        ? "REQUESTER"
                                        : answer.getUserId().equals(partnerId) ? "PARTNER" : "OTHER");
                                answerItem.put("answerSummary", truncate(answer.getAnswer(), 100));
                                return answerItem;
                            })
                            .toList());
                    return item;
                })
                .filter(Objects::nonNull)
                .limit(5)
                .toList();

        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("recentAnsweredCount", answers.size());
        promptData.put("notableAnswers", notableAnswers);
        return new QnaSummary(answers.size(), promptData);
    }

    private Map<Long, String> loadQuestionTexts(List<DailyQuestion> questions) {
        List<Long> templateIds = questions.stream()
                .map(DailyQuestion::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }

        return dailyQuestionTemplateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(
                        DailyQuestionTemplate::getTemplateId,
                        DailyQuestionTemplate::getQuestion
                ));
    }

    private MemorySummary loadMemorySummary(Long coupleId, LocalDate today) {
        List<Memory> memories = memoryRepository
                .findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
                        coupleId,
                        today.minusDays(MEMORY_WINDOW_DAYS),
                        today
                );
        Map<String, Long> tagCounts = new HashMap<>();
        Map<String, Long> placeCounts = new HashMap<>();

        for (Memory memory : memories) {
            for (MemoryAiTag tag : memory.getAiTags()) {
                if (StringUtils.hasText(tag.getTagName())) {
                    tagCounts.merge(tag.getTagName().trim(), 1L, Long::sum);
                }
            }
            String placeName = resolvePlaceName(memory.getPhotoMetadata());
            if (StringUtils.hasText(placeName)) {
                placeCounts.merge(placeName, 1L, Long::sum);
            }
        }

        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("periodDays", MEMORY_WINDOW_DAYS);
        promptData.put("recentMemoryCount", memories.size());
        promptData.put("topTags", topKeys(tagCounts, 8));
        promptData.put("frequentPlaces", topKeys(placeCounts, 5));
        promptData.put("recentMemos", memories.stream()
                .sorted(Comparator.comparing(Memory::getMemoryDate).reversed())
                .map(Memory::getMemo)
                .filter(StringUtils::hasText)
                .map(memo -> truncate(memo, 100))
                .limit(5)
                .toList());
        return new MemorySummary(memories.size(), promptData);
    }

    private List<Map<String, Object>> limitedRecentMessages(
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysesByMessageId
    ) {
        return textMessages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt).reversed())
                .limit(RECENT_MESSAGE_LIMIT)
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(message -> {
                    EmotionAnalysis analysis = analysesByMessageId.get(message.getId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("senderRole", "USER_" + message.getSenderId());
                    item.put("content", truncate(message.getContent(), MESSAGE_MAX_LENGTH));
                    item.put("emotion", analysis == null || analysis.getEmotionType() == null
                            ? null
                            : analysis.getEmotionType().name());
                    item.put("riskLevel", analysis == null || analysis.getRiskLevel() == null
                            ? null
                            : analysis.getRiskLevel().name());
                    item.put("createdAt", message.getCreatedAt());
                    return item;
                })
                .toList();
    }

    private String resolvePlaceName(MemoryPhotoMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        if (StringUtils.hasText(metadata.getPlaceName())) {
            return metadata.getPlaceName().trim();
        }
        if (StringUtils.hasText(metadata.getLocationName())) {
            return metadata.getLocationName().trim();
        }
        if (StringUtils.hasText(metadata.getAddressName())) {
            return metadata.getAddressName().trim();
        }
        return null;
    }

    private List<String> topKeys(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private record QnaSummary(int answerCount, Map<String, Object> promptData) {
    }

    private record MemorySummary(int memoryCount, Map<String, Object> promptData) {
    }
}
