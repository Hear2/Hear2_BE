package com.hear2.report.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryPhotoMetadata;
import com.hear2.memory.repository.MemoryRepository;
import com.hear2.report.dto.EmotionFlowPointResponse;
import com.hear2.report.dto.EmotionSummaryResponse;
import com.hear2.report.dto.MemoryHighlightResponse;
import com.hear2.report.dto.MonthlyComparisonResponse;
import com.hear2.report.dto.MonthlyRecommendationResponse;
import com.hear2.report.dto.MostUsedWordResponse;
import com.hear2.report.dto.RelationshipHealthCategoryResponse;
import com.hear2.report.dto.RelationshipHealthResponse;
import com.hear2.report.dto.ReportResponse;
import com.hear2.report.support.ReportPeriod;
import com.hear2.report.support.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final List<String> KOREAN_WEEKDAY_LABELS = List.of("월", "화", "수", "목", "금", "토", "일");
    private static final int MOST_USED_WORD_LIMIT = 12;
    private static final int MEMORY_HIGHLIGHT_RANGE_DAYS = 7;
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\p{L}\\p{Nd}]");
    private static final Set<String> DEFAULT_STOP_WORDS = new LinkedHashSet<>(List.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it", "of", "on", "or", "that", "the", "this", "to", "was", "we",
            "나", "난", "내", "내가", "너", "넌", "네", "니", "니가", "우리", "우린", "저", "제가", "그", "이", "저기",
            "은", "는", "이", "가", "을", "를", "에", "의", "와", "과", "도", "만", "좀", "진짜로",
            "그리고", "그래서", "근데", "그런데", "하지만", "그냥", "약간", "정말", "너무", "오늘", "어제", "내일",
            "응", "어", "아", "오", "음", "엉", "웅", "ㅎㅎ", "ㅋㅋ", "ㅠㅠ", "ㅜㅜ"
    ));

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final MemoryRepository memoryRepository;
    private final ReportNarrationService reportNarrationService;
    private final ReportAiContentService reportAiContentService;

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long coupleId, ReportType reportType, LocalDate anchorDate) {
        validateCoupleId(coupleId);

        ReportType resolvedReportType = reportType == null ? ReportType.WEEKLY : reportType;
        LocalDate resolvedAnchorDate = anchorDate == null ? LocalDate.now() : anchorDate;
        ReportPeriod period = buildPeriod(resolvedReportType, resolvedAnchorDate);
        ReportMetrics currentMetrics = collectMetrics(coupleId, period);

        EmotionType dominantEmotionType = resolveDominantEmotionType(currentMetrics.analysisByMessageId().values());
        EmotionFlowPointResponse peakPoint = resolvePeakEmotionPoint(currentMetrics.emotionFlow());
        MemoryHighlightResponse memoryHighlight = period.getReportType() == ReportType.WEEKLY
                ? buildMemoryHighlight(coupleId, period)
                : null;

        MonthlyComparisonResponse monthlyComparison = null;
        RelationshipHealthResponse relationshipHealth = null;
        List<MonthlyRecommendationResponse> monthlyRecommendations = null;
        List<MonthlyRecommendationResponse> fallbackMonthlyRecommendations = null;
        if (resolvedReportType == ReportType.MONTHLY) {
            ReportPeriod previousMonthlyPeriod = buildPeriod(ReportType.MONTHLY, period.getStartDate().minusDays(1));
            ReportMetrics previousMetrics = collectMetrics(coupleId, previousMonthlyPeriod);
            monthlyComparison = buildMonthlyComparison(currentMetrics, previousMetrics);
            relationshipHealth = buildRelationshipHealth(currentMetrics);
            fallbackMonthlyRecommendations = buildFallbackMonthlyRecommendations(currentMetrics, monthlyComparison);
        }

        String fallbackSummary = buildFallbackSummary(period, currentMetrics, dominantEmotionType);
        ReportAiContentService.ReportAiContentResult aiContent = reportAiContentService.generate(
                        resolvedReportType,
                        buildLlmInput(
                                period,
                                currentMetrics,
                                monthlyComparison,
                                relationshipHealth,
                                memoryHighlight
                        )
                )
                .orElse(null);

        String summary = aiContent != null && StringUtils.hasText(aiContent.summary())
                ? aiContent.summary()
                : fallbackSummary;
        if (resolvedReportType == ReportType.MONTHLY) {
            monthlyRecommendations = aiContent != null
                    && aiContent.recommendations() != null
                    && !aiContent.recommendations().isEmpty()
                    ? aiContent.recommendations()
                    : fallbackMonthlyRecommendations;
        }

        return ReportResponse.builder()
                .reportType(period.getReportType())
                .anchorDate(period.getAnchorDate())
                .periodStartDate(period.getStartDate())
                .periodEndDate(period.getEndDate())
                .periodStart(period.getStartDate())
                .periodEnd(period.getEndDate())
                .periodLabel(period.getPeriodLabel())
                .title(period.getTitle())
                .summary(summary)
                .chartTitle("감정 흐름")
                .chartSubtitle(period.getReportType() == ReportType.WEEKLY ? "날짜별 감정 분석 요약" : "주차별 감정 분석 요약")
                .emotionFlow(currentMetrics.emotionFlow())
                .peakEmotionLabel(peakPoint.getLabel())
                .peakEmotionScore(peakPoint.getScore())
                .totalConversationCount(currentMetrics.messages().size())
                .positiveRatio(currentMetrics.positiveRatio())
                .analyzedConversationCount(currentMetrics.analyzedConversationCount())
                .conflictCount(currentMetrics.judgeHistories().size())
                .uploadedPhotoCount(currentMetrics.uploadedPhotoCount())
                .mostUsedWords(currentMetrics.mostUsedWords())
                .oneAnswerResponseCount(null)
                .oneAnswerTotalCount(null)
                .dominantEmotionType(dominantEmotionType == null ? null : dominantEmotionType.name())
                .dominantEmotionLabel(dominantEmotionType == null ? "데이터 없음" : dominantEmotionType.getDisplayName())
                .dominantEmotionEmoji(dominantEmotionType == null ? "🙂" : dominantEmotionType.getEmoji())
                .memoryHighlight(memoryHighlight)
                .monthlyComparison(monthlyComparison)
                .relationshipHealth(relationshipHealth)
                .monthlyRecommendations(monthlyRecommendations)
                .shareTitle(period.getTitle())
                .shareMessage(summary)
                .build();
    }

    private ReportMetrics collectMetrics(Long coupleId, ReportPeriod period) {
        LocalDateTime startAt = period.getStartDate().atStartOfDay();
        LocalDateTime endExclusive = period.getEndDate().plusDays(1).atStartOfDay();

        List<ChatMessage> messages = chatMessageRepository
                .findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        coupleId,
                        startAt,
                        endExclusive
                );

        Map<Long, EmotionAnalysis> analysisByMessageId = findEmotionAnalysisMap(messages);
        List<JudgeHistory> judgeHistories = judgeHistoryRepository
                .findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        coupleId,
                        startAt,
                        endExclusive
                );
        List<EmotionFlowPointResponse> emotionFlow = buildEmotionFlow(period, messages, analysisByMessageId);
        long analyzedConversationCount = countAnalyzedTextMessages(messages, analysisByMessageId);
        int positiveRatio = calculatePositiveRatio(messages, analysisByMessageId);
        long uploadedPhotoCount = memoryRepository.countByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqual(
                coupleId,
                period.getStartDate(),
                period.getEndDate()
        );
        List<MostUsedWordResponse> mostUsedWords = calculateMostUsedWords(messages);

        return new ReportMetrics(
                messages,
                analysisByMessageId,
                judgeHistories,
                emotionFlow,
                analyzedConversationCount,
                positiveRatio,
                uploadedPhotoCount,
                mostUsedWords
        );
    }

    private Map<Long, EmotionAnalysis> findEmotionAnalysisMap(List<ChatMessage> messages) {
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        if (messageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, EmotionAnalysis> analysisByMessageId = new HashMap<>();
        for (EmotionAnalysis analysis : emotionAnalysisRepository.findByMessageIdIn(messageIds)) {
            analysisByMessageId.put(analysis.getMessage().getId(), analysis);
        }
        return analysisByMessageId;
    }

    private List<EmotionFlowPointResponse> buildEmotionFlow(
            ReportPeriod period,
            List<ChatMessage> messages,
            Map<Long, EmotionAnalysis> analysisByMessageId
    ) {
        if (period.getReportType() == ReportType.MONTHLY) {
            return buildMonthlyEmotionFlow(period, messages, analysisByMessageId);
        }
        return buildWeeklyEmotionFlow(period, messages, analysisByMessageId);
    }

    private List<EmotionFlowPointResponse> buildWeeklyEmotionFlow(
            ReportPeriod period,
            List<ChatMessage> messages,
            Map<Long, EmotionAnalysis> analysisByMessageId
    ) {
        List<EmotionFlowPointResponse> points = new ArrayList<>();
        Map<LocalDate, List<EmotionAnalysis>> analysesByDate = groupAnalysesByDate(messages, analysisByMessageId);

        for (int i = 0; i < 7; i++) {
            LocalDate date = period.getStartDate().plusDays(i);
            points.add(buildWeeklyFlowPoint(
                    KOREAN_WEEKDAY_LABELS.get(i),
                    date,
                    analysesByDate.getOrDefault(date, List.of())
            ));
        }

        return points;
    }

    private List<EmotionFlowPointResponse> buildMonthlyEmotionFlow(
            ReportPeriod period,
            List<ChatMessage> messages,
            Map<Long, EmotionAnalysis> analysisByMessageId
    ) {
        List<EmotionFlowPointResponse> points = new ArrayList<>();
        Map<LocalDate, List<EmotionAnalysis>> analysesByDate = groupAnalysesByDate(messages, analysisByMessageId);
        LocalDate cursor = period.getStartDate();
        int bucketIndex = 1;

        while (!cursor.isAfter(period.getEndDate())) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd = cursor.plusDays(6);
            if (bucketEnd.isAfter(period.getEndDate())) {
                bucketEnd = period.getEndDate();
            }

            points.add(buildMonthlyFlowPoint(
                    bucketIndex + "주",
                    bucketStart,
                    bucketEnd,
                    collectAnalyses(analysesByDate, bucketStart, bucketEnd)
            ));

            cursor = bucketEnd.plusDays(1);
            bucketIndex++;
        }

        return points;
    }

    private int calculatePositiveRatio(List<ChatMessage> messages, Map<Long, EmotionAnalysis> analysisByMessageId) {
        long analyzedMessageCount = countAnalyzedTextMessages(messages, analysisByMessageId);
        if (analyzedMessageCount == 0) {
            return 0;
        }

        long positiveCount = messages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .map(ChatMessage::getId)
                .map(analysisByMessageId::get)
                .filter(Objects::nonNull)
                .filter(analysis -> analysis.getEmotionType() != null && !analysis.getEmotionType().isNegative())
                .count();

        return (int) Math.round((positiveCount * 100.0) / analyzedMessageCount);
    }

    private EmotionType resolveDominantEmotionType(Iterable<EmotionAnalysis> analyses) {
        Map<EmotionType, Integer> counts = new EnumMap<>(EmotionType.class);
        for (EmotionAnalysis analysis : analyses) {
            EmotionType emotionType = analysis.getEmotionType();
            if (emotionType == null) {
                continue;
            }
            counts.merge(emotionType, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Map<LocalDate, List<EmotionAnalysis>> groupAnalysesByDate(
            List<ChatMessage> messages,
            Map<Long, EmotionAnalysis> analysisByMessageId
    ) {
        Map<LocalDate, List<EmotionAnalysis>> analysesByDate = new HashMap<>();
        for (ChatMessage message : messages) {
            if (message.getMessageType() != MessageType.TEXT) {
                continue;
            }
            EmotionAnalysis analysis = analysisByMessageId.get(message.getId());
            if (analysis == null) {
                continue;
            }

            LocalDate date = message.getCreatedAt().toLocalDate();
            analysesByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(analysis);
        }
        return analysesByDate;
    }

    private ReportPeriod buildPeriod(ReportType reportType, LocalDate anchorDate) {
        if (reportType == ReportType.MONTHLY) {
            LocalDate startDate = anchorDate.withDayOfMonth(1);
            LocalDate endDate = anchorDate.with(TemporalAdjusters.lastDayOfMonth());
            return ReportPeriod.builder()
                    .reportType(reportType)
                    .anchorDate(anchorDate)
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodLabel(anchorDate.getYear() + "년 " + anchorDate.getMonthValue() + "월")
                    .title(anchorDate.getMonthValue() + "월 월간 리포트")
                    .build();
        }

        LocalDate startDate = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int weekNumber = ((startDate.getDayOfMonth() - 1) / 7) + 1;

        return ReportPeriod.builder()
                .reportType(reportType)
                .anchorDate(anchorDate)
                .startDate(startDate)
                .endDate(endDate)
                .periodLabel(formatWeeklyPeriodLabel(startDate, endDate))
                .title(startDate.getMonthValue() + "월 " + toKoreanOrdinal(weekNumber) + " 주 리포트")
                .build();
    }

    private String formatWeeklyPeriodLabel(LocalDate startDate, LocalDate endDate) {
        return "%d/%d (%s) - %d/%d (%s)".formatted(
                startDate.getMonthValue(),
                startDate.getDayOfMonth(),
                toKoreanWeekday(startDate.getDayOfWeek()),
                endDate.getMonthValue(),
                endDate.getDayOfMonth(),
                toKoreanWeekday(endDate.getDayOfWeek())
        );
    }

    private String toKoreanOrdinal(int weekNumber) {
        return switch (weekNumber) {
            case 1 -> "첫째";
            case 2 -> "둘째";
            case 3 -> "셋째";
            case 4 -> "넷째";
            case 5 -> "다섯째";
            default -> weekNumber + "째";
        };
    }

    private String toKoreanWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                .replace("요일", "");
    }

    private EmotionFlowPointResponse buildWeeklyFlowPoint(
            String label,
            LocalDate date,
            List<EmotionAnalysis> analyses
    ) {
        FlowAggregate aggregate = aggregateFlow(analyses);

        return EmotionFlowPointResponse.builder()
                .label(label)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .dominantEmotion(aggregate.dominantEmotion())
                .positiveRatio(aggregate.positiveRatio())
                .negativeRatio(aggregate.negativeRatio())
                .messageCount(aggregate.messageCount())
                .score(aggregate.positiveRatio())
                .build();
    }

    private EmotionFlowPointResponse buildMonthlyFlowPoint(
            String weekLabel,
            LocalDate startDate,
            LocalDate endDate,
            List<EmotionAnalysis> analyses
    ) {
        FlowAggregate aggregate = aggregateFlow(analyses);

        return EmotionFlowPointResponse.builder()
                .label(weekLabel)
                .weekLabel(weekLabel)
                .startDate(startDate)
                .endDate(endDate)
                .date(startDate)
                .dominantEmotion(aggregate.dominantEmotion())
                .positiveRatio(aggregate.positiveRatio())
                .negativeRatio(aggregate.negativeRatio())
                .messageCount(aggregate.messageCount())
                .score(aggregate.positiveRatio())
                .build();
    }

    private FlowAggregate aggregateFlow(List<EmotionAnalysis> analyses) {
        long messageCount = analyses.size();
        if (messageCount == 0) {
            return new FlowAggregate(null, 0, 0, 0);
        }

        long positiveCount = analyses.stream()
                .filter(analysis -> analysis.getEmotionType() != null && !analysis.getEmotionType().isNegative())
                .count();
        long negativeCount = analyses.stream()
                .filter(analysis -> analysis.getEmotionType() != null && analysis.getEmotionType().isNegative())
                .count();

        return new FlowAggregate(
                toEmotionSummary(resolveDominantEmotionType(analyses)),
                (int) Math.round((positiveCount * 100.0) / messageCount),
                (int) Math.round((negativeCount * 100.0) / messageCount),
                messageCount
        );
    }

    private EmotionFlowPointResponse resolvePeakEmotionPoint(List<EmotionFlowPointResponse> emotionFlow) {
        return emotionFlow.stream()
                .max(Comparator
                        .comparingInt(EmotionFlowPointResponse::getScore)
                        .thenComparingLong(EmotionFlowPointResponse::getMessageCount))
                .orElse(EmotionFlowPointResponse.builder()
                        .label("")
                        .score(0)
                        .messageCount(0)
                        .build());
    }

    private long countAnalyzedTextMessages(List<ChatMessage> messages, Map<Long, EmotionAnalysis> analysisByMessageId) {
        return messages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .map(ChatMessage::getId)
                .map(analysisByMessageId::get)
                .filter(Objects::nonNull)
                .count();
    }

    private List<MostUsedWordResponse> calculateMostUsedWords(List<ChatMessage> messages) {
        Map<String, Long> tokenCounts = new HashMap<>();

        messages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .map(ChatMessage::getContent)
                .filter(Objects::nonNull)
                .forEach(content -> tokenize(content).forEach(token -> tokenCounts.merge(token, 1L, Long::sum)));

        return tokenCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MOST_USED_WORD_LIMIT)
                .map(entry -> MostUsedWordResponse.builder()
                        .word(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<String> tokenize(String content) {
        List<String> tokens = new ArrayList<>();
        for (String rawToken : TOKEN_SPLIT_PATTERN.split(content.trim())) {
            String normalized = NON_WORD_PATTERN.matcher(rawToken).replaceAll("").trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            if (DEFAULT_STOP_WORDS.contains(normalized)) {
                continue;
            }
            tokens.add(normalized);
        }
        return tokens;
    }

    private List<EmotionAnalysis> collectAnalyses(
            Map<LocalDate, List<EmotionAnalysis>> analysesByDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<EmotionAnalysis> analyses = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            analyses.addAll(analysesByDate.getOrDefault(cursor, List.of()));
            cursor = cursor.plusDays(1);
        }
        return analyses;
    }

    private EmotionSummaryResponse toEmotionSummary(EmotionType emotionType) {
        if (emotionType == null) {
            return null;
        }

        return EmotionSummaryResponse.builder()
                .type(emotionType.name())
                .label(emotionType.getDisplayName())
                .emoji(emotionType.getEmoji())
                .build();
    }

    private MemoryHighlightResponse buildMemoryHighlight(Long coupleId, ReportPeriod period) {
        LocalDate targetDate = period.getAnchorDate().minusYears(1);
        List<Memory> candidates = memoryRepository
                .findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
                        coupleId,
                        targetDate.minusDays(MEMORY_HIGHLIGHT_RANGE_DAYS),
                        targetDate.plusDays(MEMORY_HIGHLIGHT_RANGE_DAYS)
                );

        return candidates.stream()
                .min(Comparator.<Memory>comparingLong(
                                memory -> Math.abs(memory.getMemoryDate().toEpochDay() - targetDate.toEpochDay())
                        )
                        .thenComparing(Memory::getMemoryDate))
                .map(this::toMemoryHighlight)
                .orElse(null);
    }

    private MemoryHighlightResponse toMemoryHighlight(Memory memory) {
        MemoryPhotoMetadata metadata = memory.getPhotoMetadata();
        String location = resolveMemoryLocation(metadata);
        String title = buildMemoryHighlightTitle(memory, location);

        return MemoryHighlightResponse.builder()
                .label("1년 전 오늘")
                .title(title)
                .date(memory.getMemoryDate())
                .location(location)
                .thumbnailUrl(memory.getId() == null ? null : "/api/v1/memories/items/" + memory.getId() + "/photo")
                .build();
    }

    private String buildMemoryHighlightTitle(Memory memory, String location) {
        if (StringUtils.hasText(memory.getMemo())) {
            String memo = memory.getMemo().trim();
            return memo.endsWith("추억 보러가기") ? memo : memo + " 추억 보러가기";
        }
        if (StringUtils.hasText(location)) {
            return location + " 추억 보러가기";
        }
        if (StringUtils.hasText(memory.getOriginalFileName())) {
            return memory.getOriginalFileName() + " 추억 보러가기";
        }
        return "추억 보러가기";
    }

    private String resolveMemoryLocation(MemoryPhotoMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        if (StringUtils.hasText(metadata.getLocationName())) {
            return metadata.getLocationName().trim();
        }
        if (StringUtils.hasText(metadata.getPlaceName())) {
            return metadata.getPlaceName().trim();
        }
        if (StringUtils.hasText(metadata.getAddressName())) {
            return metadata.getAddressName().trim();
        }
        return null;
    }

    private MonthlyComparisonResponse buildMonthlyComparison(ReportMetrics currentMetrics, ReportMetrics previousMetrics) {
        return MonthlyComparisonResponse.builder()
                .conversationChangeRate(calculateChangeRate(
                        currentMetrics.messages().size(),
                        previousMetrics.messages().size()
                ))
                .positiveRatioChangeRate(calculateChangeRate(
                        currentMetrics.positiveRatio(),
                        previousMetrics.positiveRatio()
                ))
                .photoChangeRate(calculateChangeRate(
                        currentMetrics.uploadedPhotoCount(),
                        previousMetrics.uploadedPhotoCount()
                ))
                .conflictChangeRate(calculateChangeRate(
                        currentMetrics.judgeHistories().size(),
                        previousMetrics.judgeHistories().size()
                ))
                .build();
    }

    private Integer calculateChangeRate(long current, long previous) {
        if (previous == 0L) {
            return current == 0L ? 0 : 100;
        }
        return (int) Math.round(((current - previous) * 100.0) / previous);
    }

    private RelationshipHealthResponse buildRelationshipHealth(ReportMetrics currentMetrics) {
        double conversationContribution = normalizeRatio(currentMetrics.messages().size(), 240) * 0.35;
        double memoryContribution = normalizeRatio(currentMetrics.uploadedPhotoCount(), 12) * 0.20;
        double emotionContribution = normalizeRatio(currentMetrics.positiveRatio(), 100) * 0.30;
        double conflictContribution = Math.max(0.0, 1.0 - normalizeRatio(currentMetrics.judgeHistories().size(), 6)) * 0.15;

        double totalContribution = conversationContribution + memoryContribution + emotionContribution + conflictContribution;
        int score = clampToPercentage(totalContribution * 100.0);

        return RelationshipHealthResponse.builder()
                .score(score)
                .categories(List.of(
                        RelationshipHealthCategoryResponse.builder()
                                .name("대화")
                                .ratio(toContributionRatio(conversationContribution, totalContribution))
                                .build(),
                        RelationshipHealthCategoryResponse.builder()
                                .name("추억")
                                .ratio(toContributionRatio(memoryContribution, totalContribution))
                                .build(),
                        RelationshipHealthCategoryResponse.builder()
                                .name("감정")
                                .ratio(toContributionRatio(emotionContribution, totalContribution))
                                .build(),
                        RelationshipHealthCategoryResponse.builder()
                                .name("갈등")
                                .ratio(toContributionRatio(conflictContribution, totalContribution))
                                .build()
                ))
                .build();
    }

    private List<MonthlyRecommendationResponse> buildFallbackMonthlyRecommendations(
            ReportMetrics currentMetrics,
            MonthlyComparisonResponse monthlyComparison
    ) {
        List<MonthlyRecommendationResponse> recommendations = new ArrayList<>();

        long weekendMessages = currentMetrics.messages().stream()
                .filter(message -> {
                    DayOfWeek dayOfWeek = message.getCreatedAt().getDayOfWeek();
                    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
                })
                .count();

        if (weekendMessages < Math.max(4, currentMetrics.messages().size() / 5)) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("HEART")
                    .title("주말 데이트 빈도 +1")
                    .description("주말 대화가 적었어요. 토요일이나 일요일에 함께 보내는 시간을 한 번 더 만들어보세요.")
                    .build());
        }

        if (currentMetrics.uploadedPhotoCount() < 4) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("CAMERA")
                    .title("함께 사진 챌린지")
                    .description("이번 달 추억 기록이 적었어요. 한 주에 한 장씩 같이 남겨보면 다음 리포트가 더 풍성해져요.")
                    .build());
        }

        if (currentMetrics.positiveRatio() < 65) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("CHAT")
                    .title("감정 체크인 10분")
                    .description("대화 톤을 부드럽게 맞추기 위해 하루 10분 정도 서로의 기분을 먼저 물어보는 시간을 추천해요.")
                    .build());
        }

        if (currentMetrics.judgeHistories().size() > 0) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("SPARKLES")
                    .title("대화 리셋 루틴 만들기")
                    .description("민감한 대화가 있었어요. 감정이 올라올 때 잠깐 멈추고 다시 이야기하는 합의 문장을 정해보세요.")
                    .build());
        }

        if (recommendations.size() < 3 && monthlyComparison != null
                && monthlyComparison.getPositiveRatioChangeRate() != null
                && monthlyComparison.getPositiveRatioChangeRate() > 0) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("MAP")
                    .title("좋았던 리듬 이어가기")
                    .description("전월보다 분위기가 좋아졌어요. 이번 달에 좋았던 장소나 대화 패턴을 한 번 더 반복해보세요.")
                    .build());
        }

        if (recommendations.isEmpty()) {
            recommendations.add(MonthlyRecommendationResponse.builder()
                    .iconType("HEART")
                    .title("지금의 리듬 유지하기")
                    .description("대화, 추억, 감정 흐름이 안정적이에요. 이번 달의 좋은 패턴을 다음 달에도 이어가 보세요.")
                    .build());
        }

        return recommendations.stream()
                .limit(3)
                .toList();
    }

    private String buildFallbackSummary(ReportPeriod period, ReportMetrics currentMetrics, EmotionType dominantEmotionType) {
        String baseSummary = reportNarrationService.buildFallbackSummary(
                period,
                currentMetrics.emotionFlow(),
                currentMetrics.messages().size(),
                currentMetrics.positiveRatio(),
                currentMetrics.judgeHistories().size(),
                dominantEmotionType
        );

        if (period.getReportType() == ReportType.MONTHLY) {
            String seasonEmoji = seasonEmoji(period.getAnchorDate().getMonth());
            return period.getAnchorDate().getMonthValue()
                    + "월은 "
                    + baseSummary
                    + " "
                    + seasonEmoji;
        }

        if (!currentMetrics.mostUsedWords().isEmpty()) {
            return baseSummary + " 자주 나온 말은 '" + currentMetrics.mostUsedWords().get(0).getWord() + "'이었어요.";
        }

        return baseSummary;
    }

    private Map<String, Object> buildLlmInput(
            ReportPeriod period,
            ReportMetrics currentMetrics,
            MonthlyComparisonResponse monthlyComparison,
            RelationshipHealthResponse relationshipHealth,
            MemoryHighlightResponse memoryHighlight
    ) {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", period.getReportType());
        input.put("periodStart", period.getStartDate());
        input.put("periodEnd", period.getEndDate());
        input.put("totalConversationCount", currentMetrics.messages().size());
        input.put("positiveRatio", currentMetrics.positiveRatio());
        input.put("uploadedPhotoCount", currentMetrics.uploadedPhotoCount());
        input.put("mostUsedWords", currentMetrics.mostUsedWords());
        input.put("emotionFlow", currentMetrics.emotionFlow());
        input.put("monthlyComparison", monthlyComparison);
        input.put("relationshipHealth", relationshipHealth);
        input.put("memoryHighlight", memoryHighlight);
        input.put("oneAnswerResponseCount", "미연동");
        return input;
    }

    private String seasonEmoji(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "🌷";
            case JUNE, JULY, AUGUST -> "☀️";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "🍂";
            default -> "❄️";
        };
    }

    private double normalizeRatio(long current, long baseline) {
        if (baseline <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, current / (double) baseline));
    }

    private double normalizeRatio(int current, int baseline) {
        if (baseline <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, current / (double) baseline));
    }

    private int toContributionRatio(double contribution, double total) {
        if (total <= 0.0) {
            return 0;
        }
        return clampToPercentage((contribution / total) * 100.0);
    }

    private int clampToPercentage(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private void validateCoupleId(Long coupleId) {
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
    }

    private record FlowAggregate(
            EmotionSummaryResponse dominantEmotion,
            int positiveRatio,
            int negativeRatio,
            long messageCount
    ) {
    }

    private record ReportMetrics(
            List<ChatMessage> messages,
            Map<Long, EmotionAnalysis> analysisByMessageId,
            List<JudgeHistory> judgeHistories,
            List<EmotionFlowPointResponse> emotionFlow,
            long analyzedConversationCount,
            int positiveRatio,
            long uploadedPhotoCount,
            List<MostUsedWordResponse> mostUsedWords
    ) {
    }
}
