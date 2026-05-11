package com.hear2.judge.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.client.JudgeAnalysisClient;
import com.hear2.judge.dto.ConflictPatternResponse;
import com.hear2.judge.dto.JudgeFastApiMessage;
import com.hear2.judge.dto.JudgeFastApiRequest;
import com.hear2.judge.dto.JudgeHistoryResponse;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.judge.support.JudgeTriggerPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeService {

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final JudgeAnalysisClient judgeAnalysisClient;

    @Transactional
    public JudgeResponse judge(JudgeRequest request) {
        validateJudgeRequest(request);

        ChatMessage triggerMessage = chatMessageRepository.findById(request.getTriggerMessageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "trigger message not found"));
        if (!request.getCoupleId().equals(triggerMessage.getCoupleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trigger message does not belong to couple");
        }

        EmotionAnalysis triggerEmotion = findTriggerEmotionAnalysis(triggerMessage.getId());
        RiskLevel triggerRiskLevel = triggerEmotion.getRiskLevel();
        if (!JudgeTriggerPolicy.isJudgeAvailable(triggerRiskLevel, triggerEmotion.getNegativeScore())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI judge is available only for high negative score or WARNING/DANGER risk messages");
        }

        List<ChatMessage> recentMessages = findRecentTextMessages(request.getCoupleId());
        if (recentMessages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no text messages to judge");
        }

        Map<Long, EmotionAnalysis> emotionsByMessageId = findEmotionsByMessageId(recentMessages);
        List<JudgeFastApiMessage> fastApiMessages = recentMessages.stream()
                .map(message -> toFastApiMessage(message, emotionsByMessageId.get(message.getId())))
                .toList();

        JudgeFastApiRequest fastApiRequest = JudgeFastApiRequest.builder()
                .coupleId(request.getCoupleId())
                .triggerMessageId(request.getTriggerMessageId())
                .requestedByUserId(request.getRequestedByUserId())
                .messages(fastApiMessages)
                .build();

        JudgeResponse response = judgeAnalysisClient.requestJudgement(fastApiRequest);
        ConflictType conflictType = response.resolvedConflictType();
        JudgeTone judgeTone = response.resolvedJudgeTone();

        JudgeHistory savedHistory = judgeHistoryRepository.save(
                JudgeHistory.builder()
                        .coupleId(request.getCoupleId())
                        .triggerMessageId(request.getTriggerMessageId())
                        .triggerRiskLevel(triggerRiskLevel)
                        .summaryA(response.getSummaryA())
                        .summaryB(response.getSummaryB())
                        .judgement(response.getJudgement())
                        .solution(response.getSolution())
                        .reconciliationMessage(response.getReconciliationMessage())
                        .conflictType(conflictType)
                        .judgeTone(judgeTone)
                        .build()
        );
        Long sameConflictCount = judgeHistoryRepository.countByCoupleIdAndConflictType(
                request.getCoupleId(),
                conflictType
        );

        return JudgeResponse.from(savedHistory, sameConflictCount);
    }

    @Transactional(readOnly = true)
    public List<JudgeHistoryResponse> getHistories(Long coupleId) {
        validateCoupleId(coupleId);

        return judgeHistoryRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId)
                .stream()
                .map(JudgeHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConflictPatternResponse> getPatterns(Long coupleId) {
        validateCoupleId(coupleId);

        Map<ConflictType, List<JudgeHistory>> historiesByType = judgeHistoryRepository
                .findByCoupleIdOrderByCreatedAtDesc(coupleId)
                .stream()
                .collect(Collectors.groupingBy(history -> history.getConflictType() == null
                        ? ConflictType.OTHER
                        : history.getConflictType()));

        return historiesByType.entrySet()
                .stream()
                .map(entry -> ConflictPatternResponse.builder()
                        .conflictType(entry.getKey())
                        .count((long) entry.getValue().size())
                        .latestCreatedAt(entry.getValue().stream()
                                .map(JudgeHistory::getCreatedAt)
                                .max(Comparator.naturalOrder())
                                .orElse(null))
                        .build())
                .sorted(Comparator
                        .comparing(ConflictPatternResponse::getCount).reversed()
                        .thenComparing(ConflictPatternResponse::getLatestCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void validateJudgeRequest(JudgeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "judge request is required");
        }
        validateCoupleId(request.getCoupleId());
        if (request.getTriggerMessageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "triggerMessageId is required");
        }
    }

    private void validateCoupleId(Long coupleId) {
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
    }

    private EmotionAnalysis findTriggerEmotionAnalysis(Long triggerMessageId) {
        return emotionAnalysisRepository.findByMessageId(triggerMessageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "trigger message has no risk analysis"));
    }

    public static boolean isJudgeAvailable(RiskLevel riskLevel) {
        return JudgeTriggerPolicy.isJudgeAvailable(riskLevel, null);
    }

    private List<ChatMessage> findRecentTextMessages(Long coupleId) {
        List<ChatMessage> recentMessages = new ArrayList<>(
                chatMessageRepository.findTop20ByCoupleIdOrderByCreatedAtDesc(coupleId)
        );
        recentMessages.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        return recentMessages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .filter(message -> StringUtils.hasText(message.getContent()))
                .toList();
    }

    private Map<Long, EmotionAnalysis> findEmotionsByMessageId(List<ChatMessage> messages) {
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        return emotionAnalysisRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        analysis -> analysis.getMessage().getId(),
                        Function.identity()
                ));
    }

    private JudgeFastApiMessage toFastApiMessage(ChatMessage message, EmotionAnalysis emotion) {
        return JudgeFastApiMessage.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .emotionType(emotion == null ? null : emotion.getEmotionType())
                .emotionScore(emotion == null ? null : emotion.getEmotionScore())
                .negativeScore(emotion == null ? null : emotion.getNegativeScore())
                .emotionEmoji(emotion == null ? null : emotion.getEmotionEmoji())
                .riskLevel(emotion == null ? null : emotion.getRiskLevel())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
