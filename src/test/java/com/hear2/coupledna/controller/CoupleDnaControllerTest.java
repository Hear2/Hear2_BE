package com.hear2.coupledna.controller;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import org.hamcrest.Matchers;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CoupleDnaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EmotionAnalysisRepository emotionAnalysisRepository;

    @Autowired
    private JudgeHistoryRepository judgeHistoryRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @BeforeEach
    void setUp() {
        judgeHistoryRepository.deleteAll();
        emotionAnalysisRepository.deleteAll();
        chatMessageRepository.deleteAll();
        coupleMemberRepository.deleteAll();
    }

    @Test
    void returnsCoupleDnaFromChatAndEmotionData() throws Exception {
        seedCoupleDnaScenario();

        mockMvc.perform(get("/api/v1/couples/1/dna")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .param("anchorDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dnaTitle").isString())
                .andExpect(jsonPath("$.data.dnaDescription", Matchers.containsString("소통 패턴")))
                .andExpect(jsonPath("$.data.userAType", Matchers.matchesPattern("[A-Z]{4}")))
                .andExpect(jsonPath("$.data.userBType", Matchers.matchesPattern("[A-Z]{4}")))
                .andExpect(jsonPath("$.data.strengths", hasSize(3)))
                .andExpect(jsonPath("$.data.metrics", hasSize(5)))
                .andExpect(jsonPath("$.data.metrics[0].label").value("감성소통"))
                .andExpect(jsonPath("$.data.metrics[1].label").value("공감지수"))
                .andExpect(jsonPath("$.data.metrics[2].label").value("유머코드"))
                .andExpect(jsonPath("$.data.metrics[3].label").value("갈등회복"))
                .andExpect(jsonPath("$.data.metrics[4].label").value("계획성"))
                .andExpect(jsonPath("$.data.emotionScore").value(greaterThan(50)))
                .andExpect(jsonPath("$.data.empathyScore").value(greaterThan(60)))
                .andExpect(jsonPath("$.data.humorScore").value(greaterThan(20)))
                .andExpect(jsonPath("$.data.recoveryScore").value(greaterThan(40)))
                .andExpect(jsonPath("$.data.planningScore").value(greaterThan(20)))
                .andExpect(jsonPath("$.data.analyzedDays").value(8))
                .andExpect(jsonPath("$.data.generatedAt").isString())
                .andExpect(jsonPath("$.data.shareCard.badgeText").value("8일 데이터 분석 완료"))
                .andExpect(jsonPath("$.data.shareCard.headline").isString())
                .andExpect(jsonPath("$.data.shareCard.subheadline", Matchers.containsString(" x ")))
                .andExpect(jsonPath("$.data.shareCard.gradientStartColor", Matchers.matchesPattern("#[A-Fa-f0-9]{6}")))
                .andExpect(jsonPath("$.data.shareCard.gradientEndColor", Matchers.matchesPattern("#[A-Fa-f0-9]{6}")))
                .andExpect(jsonPath("$.data.shareCard.metricBadges", hasSize(2)))
                .andExpect(jsonPath("$.data.shareCard.userCards", hasSize(2)))
                .andExpect(jsonPath("$.data.shareCard.userCards[0].slot").value("USER_A"))
                .andExpect(jsonPath("$.data.shareCard.userCards[0].traits", hasSize(3)))
                .andExpect(jsonPath("$.data.shareCard.footerMessage").isString());
    }

    private void seedCoupleDnaScenario() {
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(10L)
                .role("USER_A")
                .joinedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(20L)
                .role("USER_B")
                .joinedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        createTextMessage(1L, 10L, 20L, "오늘 너무 고마워 ㅎㅎ 우리 주말 데이트 계획하자",
                LocalDateTime.of(2026, 5, 3, 10, 0), EmotionType.HAPPY, 0.91, 0.05);
        createTextMessage(1L, 20L, 10L, "좋아 토요일에 보자 나도 보고싶어",
                LocalDateTime.of(2026, 5, 3, 10, 3), EmotionType.HAPPY, 0.88, 0.06);
        createTextMessage(1L, 10L, 20L, "진짜 웃겼어 ㅋㅋ 다음 주 영화도 예약할까",
                LocalDateTime.of(2026, 5, 4, 21, 0), EmotionType.HAPPY, 0.87, 0.08);
        createTextMessage(1L, 20L, 10L, "좋아 네가 챙겨줘서 든든해",
                LocalDateTime.of(2026, 5, 4, 21, 10), EmotionType.HAPPY, 0.79, 0.10);
        createTextMessage(1L, 20L, 10L, "오늘은 조금 불안했어",
                LocalDateTime.of(2026, 5, 5, 22, 0), EmotionType.ANXIOUS, 0.42, 0.74);
        createTextMessage(1L, 10L, 20L, "괜찮아 내가 들어줄게 미안해",
                LocalDateTime.of(2026, 5, 5, 22, 12), EmotionType.HAPPY, 0.73, 0.11);
        createTextMessage(1L, 10L, 20L, "이번 주말 서울숲 가자",
                LocalDateTime.of(2026, 5, 6, 18, 0), EmotionType.HAPPY, 0.81, 0.09);
        createTextMessage(1L, 20L, 10L, "좋아 ㅎㅎ 사진도 많이 찍자",
                LocalDateTime.of(2026, 5, 6, 18, 6), EmotionType.HAPPY, 0.84, 0.08);
        createTextMessage(1L, 20L, 10L, "아까는 내가 예민했어",
                LocalDateTime.of(2026, 5, 8, 23, 0), EmotionType.ANGRY, 0.40, 0.83);
        createTextMessage(1L, 10L, 20L, "괜찮아 우리 천천히 말하자",
                LocalDateTime.of(2026, 5, 8, 23, 7), EmotionType.NEUTRAL, 0.62, 0.18);
        createTextMessage(1L, 20L, 10L, "고마워 다시 이야기하니까 마음이 풀렸어",
                LocalDateTime.of(2026, 5, 9, 9, 0), EmotionType.HAPPY, 0.86, 0.07);
        createTextMessage(1L, 10L, 20L, "다음 주 일정도 맞춰보자 ㅋㅋ",
                LocalDateTime.of(2026, 5, 10, 13, 0), EmotionType.HAPPY, 0.89, 0.06);
        createTextMessage(1L, 20L, 10L, "완전 좋아",
                LocalDateTime.of(2026, 5, 10, 13, 4), EmotionType.HAPPY, 0.83, 0.05);

        judgeHistoryRepository.save(JudgeHistory.builder()
                .coupleId(1L)
                .triggerMessageId(9L)
                .triggerRiskLevel(RiskLevel.WARNING)
                .summaryA("예민함")
                .summaryB("불안")
                .judgement("대화가 필요해요")
                .solution("천천히 마음을 확인해보기")
                .reconciliationMessage("조금 쉬고 다시 말해보자")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.SOFT)
                .createdAt(LocalDateTime.of(2026, 5, 8, 23, 1))
                .build());
    }

    private void createTextMessage(
            Long coupleId,
            Long senderId,
            Long receiverId,
            String content,
            LocalDateTime createdAt,
            EmotionType emotionType,
            double emotionScore,
            double negativeScore
    ) {
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .coupleId(coupleId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .messageType(MessageType.TEXT)
                .createdAt(createdAt)
                .build());

        emotionAnalysisRepository.save(EmotionAnalysis.builder()
                .message(message)
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

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
