package com.hear2.report.controller;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiAnalysisStatus;
import com.hear2.memory.entity.MemoryPhotoMetadata;
import com.hear2.memory.repository.MemoryRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.report.repository.ReportShareRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ReportShareRepository reportShareRepository;

    @Autowired
    private JudgeHistoryRepository judgeHistoryRepository;

    @Autowired
    private EmotionAnalysisRepository emotionAnalysisRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @BeforeEach
    void setUp() {
        reportShareRepository.deleteAll();
        memoryRepository.deleteAll();
        judgeHistoryRepository.deleteAll();
        emotionAnalysisRepository.deleteAll();
        chatMessageRepository.deleteAll();
    }

    @Test
    void weeklyReportAggregatesMetricsAndEmotionFlow() throws Exception {
        seedWeeklyMessages();

        mockMvc.perform(get("/api/v1/reports/couples/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .param("reportType", "WEEKLY")
                        .param("anchorDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("WEEKLY"))
                .andExpect(jsonPath("$.data.periodStartDate").value("2026-05-04"))
                .andExpect(jsonPath("$.data.periodEndDate").value("2026-05-10"))
                .andExpect(jsonPath("$.data.periodStart").value("2026-05-04"))
                .andExpect(jsonPath("$.data.periodEnd").value("2026-05-10"))
                .andExpect(jsonPath("$.data.chartTitle").value("감정 흐름"))
                .andExpect(jsonPath("$.data.chartSubtitle").value("날짜별 감정 분석 요약"))
                .andExpect(jsonPath("$.data.emotionFlow", hasSize(7)))
                .andExpect(jsonPath("$.data.peakEmotionLabel").value("토"))
                .andExpect(jsonPath("$.data.peakEmotionScore").value(100))
                .andExpect(jsonPath("$.data.totalConversationCount").value(7))
                .andExpect(jsonPath("$.data.positiveRatio").value(67))
                .andExpect(jsonPath("$.data.analyzedConversationCount").value(6))
                .andExpect(jsonPath("$.data.conflictCount").value(1))
                .andExpect(jsonPath("$.data.uploadedPhotoCount").value(2))
                .andExpect(jsonPath("$.data.mostUsedWords", hasSize(12)))
                .andExpect(jsonPath("$.data.mostUsedWords[0].word").value("진짜"))
                .andExpect(jsonPath("$.data.mostUsedWords[0].count").value(4))
                .andExpect(jsonPath("$.data.mostUsedWords[1].word").value("대화"))
                .andExpect(jsonPath("$.data.mostUsedWords[1].count").value(2))
                .andExpect(jsonPath("$.data.oneAnswerResponseCount").value(nullValue()))
                .andExpect(jsonPath("$.data.oneAnswerTotalCount").value(nullValue()))
                .andExpect(jsonPath("$.data.dominantEmotionType").value("HAPPY"))
                .andExpect(jsonPath("$.data.memoryHighlight.label").value("1년 전 오늘"))
                .andExpect(jsonPath("$.data.memoryHighlight.title").value("벚꽃놀이 추억 보러가기"))
                .andExpect(jsonPath("$.data.memoryHighlight.date").value("2025-05-09"))
                .andExpect(jsonPath("$.data.memoryHighlight.location").value("서울숲"))
                .andExpect(jsonPath("$.data.emotionFlow[0].date").value("2026-05-04"))
                .andExpect(jsonPath("$.data.emotionFlow[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.emotionFlow[0].dominantEmotion.type").value("HAPPY"))
                .andExpect(jsonPath("$.data.emotionFlow[0].positiveRatio").value(100))
                .andExpect(jsonPath("$.data.emotionFlow[0].negativeRatio").value(0))
                .andExpect(jsonPath("$.data.emotionFlow[0].messageCount").value(1))
                .andExpect(jsonPath("$.data.emotionFlow[0].score").value(100))
                .andExpect(jsonPath("$.data.emotionFlow[5].dominantEmotion.type").value("HAPPY"))
                .andExpect(jsonPath("$.data.emotionFlow[5].positiveRatio").value(100))
                .andExpect(jsonPath("$.data.emotionFlow[5].messageCount").value(2))
                .andExpect(jsonPath("$.data.emotionFlow[6].negativeRatio").value(100))
                .andExpect(jsonPath("$.data.emotionFlow[6].messageCount").value(2));
    }

    @Test
    void monthlyReportUsesWeeklyBuckets() throws Exception {
        seedMonthlyMessages();

        mockMvc.perform(get("/api/v1/reports/couples/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .param("reportType", "MONTHLY")
                        .param("anchorDate", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("MONTHLY"))
                .andExpect(jsonPath("$.data.periodLabel").value("2026년 5월"))
                .andExpect(jsonPath("$.data.chartSubtitle").value("주차별 감정 분석 요약"))
                .andExpect(jsonPath("$.data.emotionFlow", hasSize(5)))
                .andExpect(jsonPath("$.data.emotionFlow[0].label").value("1주"))
                .andExpect(jsonPath("$.data.emotionFlow[4].label").value("5주"))
                .andExpect(jsonPath("$.data.emotionFlow[0].weekLabel").value("1주"))
                .andExpect(jsonPath("$.data.emotionFlow[0].startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.emotionFlow[0].endDate").value("2026-05-07"))
                .andExpect(jsonPath("$.data.emotionFlow[0].score").value(100))
                .andExpect(jsonPath("$.data.emotionFlow[0].positiveRatio").value(100))
                .andExpect(jsonPath("$.data.monthlyComparison.conversationChangeRate").value(25))
                .andExpect(jsonPath("$.data.monthlyComparison.positiveRatioChangeRate").value(60))
                .andExpect(jsonPath("$.data.monthlyComparison.photoChangeRate").value(50))
                .andExpect(jsonPath("$.data.monthlyComparison.conflictChangeRate").value(-50))
                .andExpect(jsonPath("$.data.relationshipHealth.score").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.relationshipHealth.categories", hasSize(4)))
                .andExpect(jsonPath("$.data.monthlyRecommendations", hasSize(3)))
                .andExpect(jsonPath("$.data.monthlyRecommendations[0].iconType").value("CAMERA"))
                .andExpect(jsonPath("$.data.monthlyRecommendations[1].iconType").value("SPARKLES"))
                .andExpect(jsonPath("$.data.monthlyRecommendations[2].iconType").value("MAP"));
    }

    @Test
    void sharedReportIsAvailableAsJson() throws Exception {
        seedWeeklyMessages();

        String response = mockMvc.perform(post("/api/v1/reports/shares")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "requesterId": 10,
                                  "receiverId": 20,
                                  "partnerName": "지호",
                                  "reportType": "WEEKLY",
                                  "anchorDate": "2026-05-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareCode").isString())
                .andExpect(jsonPath("$.data.shareUrl").value(org.hamcrest.Matchers.containsString("/reports/shared/")))
                .andExpect(jsonPath("$.data.notificationRequested").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shareCode = JsonPath.read(response, "$.data.shareCode");

        mockMvc.perform(get("/api/v1/reports/shares/{shareCode}", shareCode)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(20L)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.title").value("5월 첫째 주 리포트"))
                .andExpect(jsonPath("$.data.shareCode").value(shareCode))
                .andExpect(jsonPath("$.data.shareUrl").value(org.hamcrest.Matchers.containsString("/reports/shared/" + shareCode)))
                .andExpect(jsonPath("$.data.peakEmotionLabel").value("토"))
                .andExpect(jsonPath("$.data.uploadedPhotoCount").value(2))
                .andExpect(jsonPath("$.data.mostUsedWords[0].word").value("진짜"))
                .andExpect(jsonPath("$.data.memoryHighlight.title").value("벚꽃놀이 추억 보러가기"))
                .andExpect(jsonPath("$.data.oneAnswerResponseCount").value(nullValue()));

        mockMvc.perform(get("/reports/shared/{shareCode}", shareCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.title").value("5월 첫째 주 리포트"))
                .andExpect(jsonPath("$.data.shareCode").value(shareCode))
                .andExpect(jsonPath("$.data.shareUrl").value(org.hamcrest.Matchers.containsString("/reports/shared/" + shareCode)));
    }

    @Test
    void missingSharedReportReturns404Json() throws Exception {
        mockMvc.perform(get("/reports/shared/{shareCode}", "missing-code"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("shared report not found"));
    }

    private void seedWeeklyMessages() {
        createMessage(1L, 10L, 20L, "월요일 진짜 행복한 대화", LocalDateTime.of(2026, 5, 4, 9, 10),
                EmotionType.HAPPY, 0.75, 0.10);
        createMessage(1L, 20L, 10L, "목요일 진짜 차분한 대화", LocalDateTime.of(2026, 5, 7, 15, 30),
                EmotionType.NEUTRAL, 0.60, 0.15);
        createMessage(1L, 10L, 20L, "토요일 진짜 너무 좋았어", LocalDateTime.of(2026, 5, 9, 11, 5),
                EmotionType.HAPPY, 0.95, 0.05);
        createMessage(1L, 20L, 10L, "토요일 보고싶어 진짜", LocalDateTime.of(2026, 5, 9, 11, 15),
                EmotionType.HAPPY, 0.88, 0.07);
        createMessage(1L, 10L, 20L, "일요일은 조금 예민했어", LocalDateTime.of(2026, 5, 10, 20, 10),
                EmotionType.ANGRY, 0.78, 0.82);
        createMessage(1L, 20L, 10L, "일요일 미안해", LocalDateTime.of(2026, 5, 10, 20, 20),
                EmotionType.SAD, 0.63, 0.74);
        chatMessageRepository.save(ChatMessage.builder()
                .coupleId(1L)
                .senderId(10L)
                .receiverId(20L)
                .content("")
                .messageType(MessageType.IMAGE)
                .mediaUrl("/uploads/chat/test-image.jpg")
                .originalFileName("test-image.jpg")
                .mediaContentType("image/jpeg")
                .mediaSize(1024L)
                .createdAt(LocalDateTime.of(2026, 5, 8, 10, 0))
                .build());

        createMessage(2L, 30L, 40L, "다른 커플 데이터", LocalDateTime.of(2026, 5, 9, 12, 0),
                EmotionType.HAPPY, 0.99, 0.01);

        createMemory(1L, 10L, "memory-1.jpg", LocalDateTime.of(2026, 5, 4, 12, 0));
        createMemory(1L, 20L, "memory-2.jpg", LocalDateTime.of(2026, 5, 10, 18, 0));
        createMemory(1L, 10L, "memory-outside.jpg", LocalDateTime.of(2026, 5, 12, 10, 0));
        createMemory(1L, 10L, "memory-last-year.jpg", LocalDateTime.of(2025, 5, 9, 10, 0), "벚꽃놀이", "서울숲");

        judgeHistoryRepository.save(JudgeHistory.builder()
                .coupleId(1L)
                .triggerMessageId(5L)
                .triggerRiskLevel(RiskLevel.WARNING)
                .summaryA("서운함")
                .summaryB("오해")
                .judgement("대화가 필요해요")
                .solution("잠깐 쉬고 다시 이야기하기")
                .reconciliationMessage("천천히 말해보자")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.SOFT)
                .createdAt(LocalDateTime.of(2026, 5, 10, 20, 30))
                .build());
    }

    private void seedMonthlyMessages() {
        createMessage(1L, 10L, 20L, "4월 첫번째 대화", LocalDateTime.of(2026, 4, 3, 10, 0),
                EmotionType.HAPPY, 0.70, 0.10);
        createMessage(1L, 10L, 20L, "4월 둘째 대화", LocalDateTime.of(2026, 4, 12, 10, 0),
                EmotionType.NEUTRAL, 0.60, 0.10);
        createMessage(1L, 10L, 20L, "4월 셋째 갈등", LocalDateTime.of(2026, 4, 20, 10, 0),
                EmotionType.ANGRY, 0.70, 0.82);
        createMessage(1L, 10L, 20L, "4월 넷째 불안", LocalDateTime.of(2026, 4, 26, 10, 0),
                EmotionType.ANXIOUS, 0.50, 0.72);

        createMessage(1L, 10L, 20L, "첫째 주", LocalDateTime.of(2026, 5, 2, 10, 0),
                EmotionType.HAPPY, 0.70, 0.10);
        createMessage(1L, 10L, 20L, "둘째 주", LocalDateTime.of(2026, 5, 10, 10, 0),
                EmotionType.NEUTRAL, 0.52, 0.15);
        createMessage(1L, 10L, 20L, "셋째 주", LocalDateTime.of(2026, 5, 16, 10, 0),
                EmotionType.HAPPY, 0.82, 0.10);
        createMessage(1L, 10L, 20L, "넷째 주", LocalDateTime.of(2026, 5, 24, 10, 0),
                EmotionType.ANXIOUS, 0.55, 0.65);
        createMessage(1L, 10L, 20L, "다섯째 주", LocalDateTime.of(2026, 5, 30, 10, 0),
                EmotionType.HAPPY, 0.91, 0.05);

        createMemory(1L, 10L, "apr-1.jpg", LocalDateTime.of(2026, 4, 5, 12, 0));
        createMemory(1L, 20L, "apr-2.jpg", LocalDateTime.of(2026, 4, 19, 12, 0));
        createMemory(1L, 10L, "may-1.jpg", LocalDateTime.of(2026, 5, 2, 12, 0));
        createMemory(1L, 20L, "may-2.jpg", LocalDateTime.of(2026, 5, 10, 12, 0));
        createMemory(1L, 10L, "may-3.jpg", LocalDateTime.of(2026, 5, 24, 12, 0));

        judgeHistoryRepository.save(JudgeHistory.builder()
                .coupleId(1L)
                .triggerMessageId(3L)
                .triggerRiskLevel(RiskLevel.WARNING)
                .summaryA("4월 갈등")
                .summaryB("답답함")
                .judgement("대화 필요")
                .solution("감정 확인")
                .reconciliationMessage("다시 이야기하자")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.SOFT)
                .createdAt(LocalDateTime.of(2026, 4, 20, 10, 30))
                .build());
        judgeHistoryRepository.save(JudgeHistory.builder()
                .coupleId(1L)
                .triggerMessageId(4L)
                .triggerRiskLevel(RiskLevel.WARNING)
                .summaryA("4월 추가 갈등")
                .summaryB("불안")
                .judgement("정리 필요")
                .solution("천천히 말하기")
                .reconciliationMessage("오해 풀자")
                .conflictType(ConflictType.DAILY)
                .judgeTone(JudgeTone.SOFT)
                .createdAt(LocalDateTime.of(2026, 4, 26, 10, 30))
                .build());
        judgeHistoryRepository.save(JudgeHistory.builder()
                .coupleId(1L)
                .triggerMessageId(8L)
                .triggerRiskLevel(RiskLevel.WARNING)
                .summaryA("5월 갈등")
                .summaryB("예민함")
                .judgement("대화 필요")
                .solution("숨 고르기")
                .reconciliationMessage("다시 이야기해보자")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.SOFT)
                .createdAt(LocalDateTime.of(2026, 5, 24, 10, 30))
                .build());
    }

    private void createMessage(
            Long coupleId,
            Long senderId,
            Long receiverId,
            String content,
            LocalDateTime createdAt,
            EmotionType emotionType,
            double emotionScore,
            double negativeScore
    ) {
        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.builder()
                .coupleId(coupleId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .messageType(MessageType.TEXT)
                .createdAt(createdAt)
                .build());

        emotionAnalysisRepository.save(EmotionAnalysis.builder()
                .message(savedMessage)
                .emotionType(emotionType)
                .emotionScore(emotionScore)
                .negativeScore(negativeScore)
                .emotionEmoji(emotionType.getEmoji())
                .riskLevel(negativeScore >= 0.8 ? RiskLevel.WARNING : RiskLevel.NONE)
                .riskDetected(negativeScore >= 0.8)
                .riskReason(negativeScore >= 0.8 ? "negative score threshold exceeded" : null)
                .detectedRiskKeywords(negativeScore >= 0.8 ? "예민" : null)
                .analyzedAt(createdAt.plusMinutes(1))
                .build());
    }

    private void createMemory(Long coupleId, Long uploaderId, String originalFileName, LocalDateTime memoryDateTime) {
        createMemory(coupleId, uploaderId, originalFileName, memoryDateTime, "사진", null);
    }

    private void createMemory(
            Long coupleId,
            Long uploaderId,
            String originalFileName,
            LocalDateTime memoryDateTime,
            String memo,
            String locationName
    ) {
        Memory memory = Memory.builder()
                .coupleId(coupleId)
                .uploaderId(uploaderId)
                .storedPhotoPath("memory/" + originalFileName)
                .originalFileName(originalFileName)
                .photoContentType("image/jpeg")
                .photoSize(2048L)
                .memo(memo)
                .memoryDate(memoryDateTime.toLocalDate())
                .aiAnalysisStatus(MemoryAiAnalysisStatus.COMPLETED)
                .build();

        if (locationName != null) {
            memory.attachPhotoMetadata(MemoryPhotoMetadata.create(
                    memoryDateTime,
                    null,
                    null,
                    locationName,
                    locationName,
                    locationName
            ));
        }

        memoryRepository.save(memory);
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
