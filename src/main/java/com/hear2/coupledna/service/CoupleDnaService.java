package com.hear2.coupledna.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.coupledna.dto.CoupleDnaMetricResponse;
import com.hear2.coupledna.dto.CoupleDnaResponse;
import com.hear2.coupledna.dto.CoupleDnaShareCardResponse;
import com.hear2.coupledna.dto.CoupleDnaShareMetricBadgeResponse;
import com.hear2.coupledna.dto.CoupleDnaShareUserCardResponse;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.repository.JudgeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoupleDnaService {

    private static final int ANALYSIS_WINDOW_DAYS = 120;
    private static final long RECOVERY_WINDOW_HOURS = 48L;
    private static final long SUPPORT_WINDOW_HOURS = 6L;
    private static final int MAX_STRENGTHS = 3;
    private static final Set<EmotionType> NEGATIVE_EMOTIONS = EnumSet.of(
            EmotionType.SAD,
            EmotionType.ANGRY,
            EmotionType.ANXIOUS
    );
    private static final List<String> EMPATHY_KEYWORDS = List.of(
            "괜찮", "고마", "미안", "사랑", "보고싶", "이해", "고생", "수고", "힘내",
            "응원", "다행", "걱정", "들어줄", "듣고", "배려", "축하"
    );
    private static final List<String> HUMOR_KEYWORDS = List.of(
            "ㅋㅋ", "ㅎㅎ", "하하", "히히", "lol", "lmao", "웃", "농담", "장난", "개그"
    );
    private static final List<String> PLANNING_KEYWORDS = List.of(
            "내일", "모레", "주말", "다음주", "다음 주", "이번주", "이번 주", "언제", "일정",
            "계획", "약속", "예약", "보자", "만날", "가자", "준비", "출발", "시간", "데이트",
            "토요일", "일요일", "월요일", "화요일", "수요일", "목요일", "금요일"
    );

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional(readOnly = true)
    public CoupleDnaResponse getCoupleDna(Long coupleId, LocalDate anchorDate) {
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }

        LocalDate resolvedAnchorDate = anchorDate == null ? LocalDate.now() : anchorDate;
        LocalDate startDate = resolvedAnchorDate.minusDays(ANALYSIS_WINDOW_DAYS - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusive = resolvedAnchorDate.plusDays(1L).atStartOfDay();

        List<ChatMessage> messages = chatMessageRepository
                .findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        coupleId,
                        startAt,
                        endExclusive
                );
        List<ChatMessage> textMessages = messages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .toList();
        Map<Long, EmotionAnalysis> analysisByMessageId = loadAnalyses(textMessages);
        List<JudgeHistory> judgeHistories = judgeHistoryRepository
                .findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        coupleId,
                        startAt,
                        endExclusive
                );

        LocalDate actualStartDate = resolveActualStartDate(startDate, textMessages);
        int analyzedDays = calculateAnalyzedDays(actualStartDate, resolvedAnchorDate, textMessages.isEmpty());
        CoupleTextStats coupleStats = buildCoupleStats(textMessages, analysisByMessageId, actualStartDate, analyzedDays);
        List<Long> participants = resolveParticipants(coupleId, messages);

        ParticipantTextStats userAStats = buildParticipantStats(
                participants.isEmpty() ? null : participants.get(0),
                textMessages,
                analysisByMessageId,
                actualStartDate,
                analyzedDays
        );
        ParticipantTextStats userBStats = buildParticipantStats(
                participants.size() < 2 ? null : participants.get(1),
                textMessages,
                analysisByMessageId,
                actualStartDate,
                analyzedDays
        );

        int emotionScore = calculateEmotionScore(coupleStats, analyzedDays);
        int empathyScore = calculateEmpathyScore(coupleStats);
        int humorScore = calculateHumorScore(coupleStats);
        int recoveryScore = calculateRecoveryScore(coupleStats, judgeHistories, textMessages, analysisByMessageId, analyzedDays);
        int planningScore = calculatePlanningScore(coupleStats, userAStats, userBStats);

        List<CoupleDnaMetricResponse> metrics = List.of(
                metric("감성소통", emotionScore),
                metric("공감지수", empathyScore),
                metric("유머코드", humorScore),
                metric("갈등회복", recoveryScore),
                metric("계획성", planningScore)
        );
        List<CoupleDnaMetricResponse> strongestMetrics = metrics.stream()
                .sorted(Comparator.comparingInt(CoupleDnaMetricResponse::getScore).reversed()
                        .thenComparing(CoupleDnaMetricResponse::getLabel))
                .toList();

        return CoupleDnaResponse.builder()
                .dnaTitle(buildDnaTitle(strongestMetrics))
                .dnaDescription(buildDnaDescription(strongestMetrics, userAStats.type(), userBStats.type(), analyzedDays, textMessages.size()))
                .userAType(userAStats.type())
                .userBType(userBStats.type())
                .strengths(buildStrengths(strongestMetrics))
                .metrics(metrics)
                .emotionScore(emotionScore)
                .empathyScore(empathyScore)
                .humorScore(humorScore)
                .recoveryScore(recoveryScore)
                .planningScore(planningScore)
                .analyzedDays(analyzedDays)
                .generatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .shareCard(buildShareCard(strongestMetrics, userAStats.type(), userBStats.type(), analyzedDays))
                .build();
    }

    private Map<Long, EmotionAnalysis> loadAnalyses(List<ChatMessage> textMessages) {
        if (textMessages.isEmpty()) {
            return Map.of();
        }

        Map<Long, EmotionAnalysis> analysisByMessageId = new HashMap<>();
        List<Long> messageIds = textMessages.stream()
                .map(ChatMessage::getId)
                .filter(Objects::nonNull)
                .toList();

        for (EmotionAnalysis analysis : emotionAnalysisRepository.findByMessageIdIn(messageIds)) {
            analysisByMessageId.put(analysis.getMessage().getId(), analysis);
        }
        return analysisByMessageId;
    }

    private LocalDate resolveActualStartDate(LocalDate defaultStartDate, List<ChatMessage> textMessages) {
        return textMessages.stream()
                .findFirst()
                .map(message -> message.getCreatedAt().toLocalDate())
                .map(firstDate -> firstDate.isAfter(defaultStartDate) ? firstDate : defaultStartDate)
                .orElse(defaultStartDate);
    }

    private int calculateAnalyzedDays(LocalDate actualStartDate, LocalDate anchorDate, boolean noMessages) {
        if (noMessages || actualStartDate.isAfter(anchorDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(actualStartDate, anchorDate) + 1;
    }

    private List<Long> resolveParticipants(Long coupleId, List<ChatMessage> messages) {
        List<Long> participantIds = coupleMemberRepository.findByCoupleIdOrderByUserIdAsc(coupleId).stream()
                .map(CoupleMember::getUserId)
                .distinct()
                .toList();
        if (!participantIds.isEmpty()) {
            return participantIds;
        }

        return messages.stream()
                .flatMap(message -> List.of(message.getSenderId(), message.getReceiverId()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private CoupleTextStats buildCoupleStats(
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysisByMessageId,
            LocalDate actualStartDate,
            int analyzedDays
    ) {
        if (textMessages.isEmpty()) {
            return CoupleTextStats.empty(actualStartDate, analyzedDays);
        }

        Set<LocalDate> messageDays = new LinkedHashSet<>();
        Set<LocalDate> humorDays = new LinkedHashSet<>();
        Set<EmotionType> emotionTypes = EnumSet.noneOf(EmotionType.class);
        int empathyMessageCount = 0;
        int humorMessageCount = 0;
        int planningMessageCount = 0;
        int analyzedTextCount = 0;
        int positiveAnalyzedCount = 0;
        int supportTriggerCount = 0;
        int supportResponseCount = 0;

        for (int i = 0; i < textMessages.size(); i++) {
            ChatMessage message = textMessages.get(i);
            String normalized = normalize(message.getContent());
            LocalDate messageDate = message.getCreatedAt().toLocalDate();
            messageDays.add(messageDate);

            if (containsAny(normalized, EMPATHY_KEYWORDS)) {
                empathyMessageCount++;
            }
            if (containsAny(normalized, HUMOR_KEYWORDS)) {
                humorMessageCount++;
                humorDays.add(messageDate);
            }
            if (containsAny(normalized, PLANNING_KEYWORDS)) {
                planningMessageCount++;
            }

            EmotionAnalysis analysis = analysisByMessageId.get(message.getId());
            if (analysis != null) {
                analyzedTextCount++;
                if (analysis.getEmotionType() != null) {
                    emotionTypes.add(analysis.getEmotionType());
                    if (!analysis.getEmotionType().isNegative()) {
                        positiveAnalyzedCount++;
                    }
                    if (NEGATIVE_EMOTIONS.contains(analysis.getEmotionType())) {
                        supportTriggerCount++;
                        if (hasSupportiveReply(textMessages, analysisByMessageId, i, message)) {
                            supportResponseCount++;
                        }
                    }
                }
            }
        }

        return new CoupleTextStats(
                textMessages.size(),
                analyzedTextCount,
                positiveAnalyzedCount,
                messageDays.size(),
                empathyMessageCount,
                humorMessageCount,
                planningMessageCount,
                humorDays.size(),
                emotionTypes.size(),
                supportTriggerCount,
                supportResponseCount,
                actualStartDate,
                analyzedDays
        );
    }

    private boolean hasSupportiveReply(
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysisByMessageId,
            int triggerIndex,
            ChatMessage triggerMessage
    ) {
        LocalDateTime deadline = triggerMessage.getCreatedAt().plusHours(SUPPORT_WINDOW_HOURS);

        for (int i = triggerIndex + 1; i < textMessages.size(); i++) {
            ChatMessage candidate = textMessages.get(i);
            if (candidate.getCreatedAt().isAfter(deadline)) {
                break;
            }
            if (Objects.equals(candidate.getSenderId(), triggerMessage.getSenderId())) {
                continue;
            }

            String normalized = normalize(candidate.getContent());
            if (containsAny(normalized, EMPATHY_KEYWORDS)) {
                return true;
            }

            EmotionAnalysis candidateAnalysis = analysisByMessageId.get(candidate.getId());
            if (candidateAnalysis != null
                    && candidateAnalysis.getEmotionType() != null
                    && !candidateAnalysis.getEmotionType().isNegative()) {
                return true;
            }

            return false;
        }
        return false;
    }

    private ParticipantTextStats buildParticipantStats(
            Long userId,
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysisByMessageId,
            LocalDate actualStartDate,
            int analyzedDays
    ) {
        if (userId == null) {
            return ParticipantTextStats.empty(actualStartDate, analyzedDays);
        }

        List<ChatMessage> ownMessages = textMessages.stream()
                .filter(message -> Objects.equals(userId, message.getSenderId()))
                .toList();

        if (ownMessages.isEmpty()) {
            return ParticipantTextStats.empty(actualStartDate, analyzedDays);
        }

        Set<LocalDate> activeDays = new LinkedHashSet<>();
        Set<EmotionType> emotionTypes = EnumSet.noneOf(EmotionType.class);
        int empathyCount = 0;
        int humorCount = 0;
        int planningCount = 0;
        int analyzedCount = 0;
        int positiveCount = 0;
        int totalLength = 0;

        for (ChatMessage message : ownMessages) {
            activeDays.add(message.getCreatedAt().toLocalDate());
            String normalized = normalize(message.getContent());
            totalLength += normalized.length();

            if (containsAny(normalized, EMPATHY_KEYWORDS)) {
                empathyCount++;
            }
            if (containsAny(normalized, HUMOR_KEYWORDS)) {
                humorCount++;
            }
            if (containsAny(normalized, PLANNING_KEYWORDS)) {
                planningCount++;
            }

            EmotionAnalysis analysis = analysisByMessageId.get(message.getId());
            if (analysis != null) {
                analyzedCount++;
                if (analysis.getEmotionType() != null) {
                    emotionTypes.add(analysis.getEmotionType());
                    if (!analysis.getEmotionType().isNegative()) {
                        positiveCount++;
                    }
                }
            }
        }

        double averageLength = ownMessages.isEmpty() ? 0.0 : totalLength / (double) ownMessages.size();
        int positiveRatio = analyzedCount == 0 ? 0 : clampToPercentage((positiveCount * 100.0) / analyzedCount);

        return new ParticipantTextStats(
                userId,
                ownMessages.size(),
                activeDays.size(),
                empathyCount,
                humorCount,
                planningCount,
                emotionTypes.size(),
                positiveRatio,
                averageLength,
                resolveType(
                        ownMessages.size(),
                        activeDays.size(),
                        empathyCount,
                        humorCount,
                        planningCount,
                        emotionTypes.size(),
                        positiveRatio,
                        averageLength,
                        textMessages.size(),
                        analyzedDays
                )
        );
    }

    private String resolveType(
            int sentMessageCount,
            int activeDays,
            int empathyCount,
            int humorCount,
            int planningCount,
            int emotionDiversityCount,
            int positiveRatio,
            double averageLength,
            int totalTextCount,
            int analyzedDays
    ) {
        if (sentMessageCount == 0) {
            return "UNKN";
        }

        double outgoingShare = totalTextCount == 0 ? 0.0 : sentMessageCount / (double) totalTextCount;
        double eScore = scoreRatio(sentMessageCount, Math.max(1.0, analyzedDays * 1.2)) * 0.55
                + clampToPercentage(outgoingShare * 100.0) * 0.45;
        double nScore = scoreRatio(emotionDiversityCount, 4.0) * 0.60
                + scoreRatio(humorCount, Math.max(1.0, sentMessageCount * 0.08)) * 0.20
                + scoreRatio(averageLength, 18.0) * 0.20;
        double fScore = scoreRatio(empathyCount, Math.max(1.0, sentMessageCount * 0.12)) * 0.60
                + positiveRatio * 0.40;
        double jScore = scoreRatio(planningCount, Math.max(1.0, sentMessageCount * 0.10)) * 0.70
                + scoreRatio(activeDays, Math.max(1.0, analyzedDays * 0.55)) * 0.30;

        StringBuilder type = new StringBuilder(4);
        type.append(eScore >= 55 ? 'E' : 'I');
        type.append(nScore >= 55 ? 'N' : 'S');
        type.append(fScore >= 55 ? 'F' : 'T');
        type.append(jScore >= 55 ? 'J' : 'P');
        return type.toString();
    }

    private int calculateEmotionScore(CoupleTextStats stats, int analyzedDays) {
        if (stats.totalTextCount() == 0 || analyzedDays == 0) {
            return 0;
        }

        double activeDayScore = scoreRatio(stats.activeDayCount(), Math.max(1.0, analyzedDays * 0.65));
        double diversityScore = scoreRatio(stats.emotionDiversityCount(), 4.0);

        return weightedScore(
                stats.positiveRatio(), 0.45,
                diversityScore, 0.30,
                activeDayScore, 0.25
        );
    }

    private int calculateEmpathyScore(CoupleTextStats stats) {
        if (stats.totalTextCount() == 0) {
            return 0;
        }

        double empathyMessageScore = scoreRatio(stats.empathyMessageCount(), Math.max(1.0, stats.totalTextCount() * 0.12));
        double supportScore = stats.supportTriggerCount() == 0
                ? empathyMessageScore
                : scoreRatio(stats.supportResponseCount(), Math.max(1.0, stats.supportTriggerCount() * 0.70));

        return weightedScore(
                empathyMessageScore, 0.65,
                supportScore, 0.35
        );
    }

    private int calculateHumorScore(CoupleTextStats stats) {
        if (stats.totalTextCount() == 0) {
            return 0;
        }

        double humorMessageScore = scoreRatio(stats.humorMessageCount(), Math.max(1.0, stats.totalTextCount() * 0.10));
        double humorDayScore = scoreRatio(stats.humorDayCount(), Math.max(1.0, stats.activeDayCount() * 0.35));

        return weightedScore(
                humorMessageScore, 0.70,
                humorDayScore, 0.30
        );
    }

    private int calculateRecoveryScore(
            CoupleTextStats stats,
            List<JudgeHistory> judgeHistories,
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysisByMessageId,
            int analyzedDays
    ) {
        if (stats.totalTextCount() == 0) {
            return 0;
        }

        if (judgeHistories.isEmpty()) {
            double activeDayScore = scoreRatio(stats.activeDayCount(), Math.max(1.0, analyzedDays * 0.60));
            return weightedScore(
                    stats.positiveRatio(), 0.60,
                    activeDayScore, 0.40
            );
        }

        int recoveredCount = 0;
        for (JudgeHistory judgeHistory : judgeHistories) {
            if (isRecovered(judgeHistory, textMessages, analysisByMessageId)) {
                recoveredCount++;
            }
        }

        double conflictRecoveryScore = scoreRatio(recoveredCount, Math.max(1.0, judgeHistories.size() * 0.80));
        return weightedScore(
                conflictRecoveryScore, 0.75,
                stats.positiveRatio(), 0.25
        );
    }

    private boolean isRecovered(
            JudgeHistory judgeHistory,
            List<ChatMessage> textMessages,
            Map<Long, EmotionAnalysis> analysisByMessageId
    ) {
        List<ChatMessage> followUps = textMessages.stream()
                .filter(message -> message.getCreatedAt().isAfter(judgeHistory.getCreatedAt()))
                .filter(message -> !message.getCreatedAt().isAfter(judgeHistory.getCreatedAt().plusHours(RECOVERY_WINDOW_HOURS)))
                .limit(6)
                .toList();

        if (followUps.size() < 2) {
            return false;
        }

        int positiveCount = 0;
        int analyzedCount = 0;
        int empathyCount = 0;

        for (ChatMessage followUp : followUps) {
            String normalized = normalize(followUp.getContent());
            if (containsAny(normalized, EMPATHY_KEYWORDS)) {
                empathyCount++;
            }

            EmotionAnalysis analysis = analysisByMessageId.get(followUp.getId());
            if (analysis == null || analysis.getEmotionType() == null) {
                continue;
            }
            analyzedCount++;
            if (!analysis.getEmotionType().isNegative()) {
                positiveCount++;
            }
        }

        int positiveRatio = analyzedCount == 0 ? 0 : clampToPercentage((positiveCount * 100.0) / analyzedCount);
        return positiveRatio >= 60 || empathyCount >= 1;
    }

    private int calculatePlanningScore(
            CoupleTextStats stats,
            ParticipantTextStats userAStats,
            ParticipantTextStats userBStats
    ) {
        if (stats.totalTextCount() == 0) {
            return 0;
        }

        double planningMessageScore = scoreRatio(stats.planningMessageCount(), Math.max(1.0, stats.totalTextCount() * 0.10));
        double planningBalanceScore = scoreRatio(resolvePlanningBalance(userAStats, userBStats), 0.75);

        return weightedScore(
                planningMessageScore, 0.75,
                planningBalanceScore, 0.25
        );
    }

    private double resolvePlanningBalance(ParticipantTextStats userAStats, ParticipantTextStats userBStats) {
        int left = userAStats.planningCount();
        int right = userBStats.planningCount();

        if (left == 0 && right == 0) {
            return 0.0;
        }
        if (left == 0 || right == 0) {
            return 0.40;
        }
        return Math.min(left, right) / (double) Math.max(left, right);
    }

    private CoupleDnaMetricResponse metric(String label, int score) {
        return CoupleDnaMetricResponse.builder()
                .label(label)
                .score(score)
                .build();
    }

    private String buildDnaTitle(List<CoupleDnaMetricResponse> strongestMetrics) {
        if (strongestMetrics.isEmpty()) {
            return "천천히 알아가는 커플";
        }

        String topLabel = strongestMetrics.get(0).getLabel();
        String secondLabel = strongestMetrics.size() > 1 ? strongestMetrics.get(1).getLabel() : topLabel;
        return titlePrefix(topLabel) + " " + titleSuffix(secondLabel) + " 커플";
    }

    private String buildDnaDescription(
            List<CoupleDnaMetricResponse> strongestMetrics,
            String userAType,
            String userBType,
            int analyzedDays,
            int totalConversationCount
    ) {
        if (totalConversationCount == 0) {
            return "아직 분석할 대화가 많지 않아요. 대화가 쌓이면 두 분만의 DNA가 더 또렷해질 거예요.";
        }

        String topStrength = strongestMetrics.isEmpty() ? "소통" : strengthTag(strongestMetrics.get(0).getLabel());
        return "%s x %s 소통 패턴. 최근 %d일 동안 %s이(가) 특히 또렷하게 드러났어요."
                .formatted(userAType, userBType, analyzedDays, topStrength);
    }

    private CoupleDnaShareCardResponse buildShareCard(
            List<CoupleDnaMetricResponse> strongestMetrics,
            String userAType,
            String userBType,
            int analyzedDays
    ) {
        CoupleDnaMetricResponse firstMetric = strongestMetrics.isEmpty()
                ? metric("감성소통", 0)
                : strongestMetrics.get(0);
        CoupleDnaMetricResponse secondMetric = strongestMetrics.size() > 1
                ? strongestMetrics.get(1)
                : firstMetric;
        ThemePalette themePalette = resolveThemePalette(firstMetric.getLabel());

        return CoupleDnaShareCardResponse.builder()
                .badgeText(analyzedDays + "일 데이터 분석 완료")
                .headline(buildDnaTitle(strongestMetrics))
                .subheadline(userAType + " x " + userBType + " 소통 패턴")
                .gradientStartColor(themePalette.gradientStartColor())
                .gradientEndColor(themePalette.gradientEndColor())
                .metricBadges(List.of(
                        CoupleDnaShareMetricBadgeResponse.builder()
                                .label(shortMetricLabel(firstMetric.getLabel()) + " " + firstMetric.getScore() + "%")
                                .score(firstMetric.getScore())
                                .build(),
                        CoupleDnaShareMetricBadgeResponse.builder()
                                .label(shortMetricLabel(secondMetric.getLabel()) + " " + secondMetric.getScore() + "%")
                                .score(secondMetric.getScore())
                                .build()
                ))
                .userCards(List.of(
                        CoupleDnaShareUserCardResponse.builder()
                                .slot("USER_A")
                                .type(userAType)
                                .accentColor(themePalette.userAAccentColor())
                                .traits(typeTraits(userAType))
                                .build(),
                        CoupleDnaShareUserCardResponse.builder()
                                .slot("USER_B")
                                .type(userBType)
                                .accentColor(themePalette.userBAccentColor())
                                .traits(typeTraits(userBType))
                                .build()
                ))
                .footerMessage(buildShareFooter(firstMetric.getLabel(), secondMetric.getLabel()))
                .build();
    }

    private List<String> buildStrengths(List<CoupleDnaMetricResponse> strongestMetrics) {
        LinkedHashSet<String> strengths = new LinkedHashSet<>();
        for (CoupleDnaMetricResponse metric : strongestMetrics) {
            strengths.add(strengthTag(metric.getLabel()));
            if (strengths.size() >= MAX_STRENGTHS) {
                break;
            }
        }
        return new ArrayList<>(strengths);
    }

    private String buildShareFooter(String firstLabel, String secondLabel) {
        return strengthTag(firstLabel) + "과 " + strengthTag(secondLabel) + "의 리듬이 특히 또렷했던 기간이에요.";
    }

    private String titlePrefix(String label) {
        return switch (label) {
            case "감성소통" -> "감정형";
            case "공감지수" -> "공감형";
            case "유머코드" -> "유쾌한";
            case "갈등회복" -> "회복형";
            case "계획성" -> "계획형";
            default -> "따뜻한";
        };
    }

    private String titleSuffix(String label) {
        return switch (label) {
            case "감성소통" -> "소통가";
            case "공감지수" -> "연결가";
            case "유머코드" -> "탐험가";
            case "갈등회복" -> "회복가";
            case "계획성" -> "설계가";
            default -> "파트너";
        };
    }

    private String strengthTag(String label) {
        return switch (label) {
            case "감성소통" -> "열정";
            case "공감지수" -> "공감";
            case "유머코드" -> "유쾌함";
            case "갈등회복" -> "회복력";
            case "계획성" -> "계획";
            default -> "소통";
        };
    }

    private String shortMetricLabel(String label) {
        return switch (label) {
            case "감성소통" -> "감성";
            case "공감지수" -> "공감";
            case "유머코드" -> "유머";
            case "갈등회복" -> "회복";
            case "계획성" -> "계획";
            default -> label;
        };
    }

    private List<String> typeTraits(String type) {
        LinkedHashSet<String> traits = new LinkedHashSet<>();
        if (!StringUtils.hasText(type) || type.length() < 4) {
            return List.of("소통");
        }

        char[] letters = type.toUpperCase(Locale.ROOT).toCharArray();
        for (char letter : letters) {
            traits.add(switch (letter) {
                case 'E' -> "열정";
                case 'I' -> "깊이";
                case 'N' -> "직관";
                case 'S' -> "현실";
                case 'F' -> "공감";
                case 'T' -> "균형";
                case 'J' -> "계획";
                case 'P' -> "유연";
                default -> "소통";
            });
            if (traits.size() >= MAX_STRENGTHS) {
                break;
            }
        }

        return new ArrayList<>(traits);
    }

    private ThemePalette resolveThemePalette(String topLabel) {
        return switch (topLabel) {
            case "감성소통", "공감지수" -> new ThemePalette("#FF4F93", "#B69CFF", "#FF6EA8", "#5B93FF");
            case "유머코드" -> new ThemePalette("#FF6B8B", "#FFB36F", "#FF8BB7", "#6A9BFF");
            case "갈등회복" -> new ThemePalette("#4F8CFF", "#8CB8FF", "#6F7FFF", "#5BA3FF");
            case "계획성" -> new ThemePalette("#8F7DFF", "#C6B3FF", "#9F81FF", "#6F9BFF");
            default -> new ThemePalette("#FF4F93", "#B69CFF", "#FF6EA8", "#5B93FF");
        };
    }

    private String normalize(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String normalizedContent, Collection<String> keywords) {
        if (!StringUtils.hasText(normalizedContent)) {
            return false;
        }
        for (String keyword : keywords) {
            if (normalizedContent.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double scoreRatio(double actual, double target) {
        if (target <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, (actual / target) * 100.0));
    }

    private int weightedScore(double left, double leftWeight, double right, double rightWeight) {
        return clampToPercentage((left * leftWeight) + (right * rightWeight));
    }

    private int weightedScore(
            double first, double firstWeight,
            double second, double secondWeight,
            double third, double thirdWeight
    ) {
        return clampToPercentage((first * firstWeight) + (second * secondWeight) + (third * thirdWeight));
    }

    private int clampToPercentage(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private record CoupleTextStats(
            int totalTextCount,
            int analyzedTextCount,
            int positiveAnalyzedCount,
            int activeDayCount,
            int empathyMessageCount,
            int humorMessageCount,
            int planningMessageCount,
            int humorDayCount,
            int emotionDiversityCount,
            int supportTriggerCount,
            int supportResponseCount,
            LocalDate actualStartDate,
            int analyzedDays
    ) {
        private static CoupleTextStats empty(LocalDate actualStartDate, int analyzedDays) {
            return new CoupleTextStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, actualStartDate, analyzedDays);
        }

        private int positiveRatio() {
            if (analyzedTextCount == 0) {
                return 0;
            }
            return Math.max(0, Math.min(100, (int) Math.round((positiveAnalyzedCount * 100.0) / analyzedTextCount)));
        }
    }

    private record ParticipantTextStats(
            Long userId,
            int sentMessageCount,
            int activeDayCount,
            int empathyCount,
            int humorCount,
            int planningCount,
            int emotionDiversityCount,
            int positiveRatio,
            double averageLength,
            String type
    ) {
        private static ParticipantTextStats empty(LocalDate actualStartDate, int analyzedDays) {
            return new ParticipantTextStats(null, 0, 0, 0, 0, 0, 0, 0, 0.0, "UNKN");
        }
    }

    private record ThemePalette(
            String gradientStartColor,
            String gradientEndColor,
            String userAAccentColor,
            String userBAccentColor
    ) {
    }
}
