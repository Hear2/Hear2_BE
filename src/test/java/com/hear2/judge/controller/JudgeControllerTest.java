package com.hear2.judge.controller;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.judge.client.JudgeAnalysisClient;
import com.hear2.judge.entity.JudgeFeedback;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeFeedbackRepository;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JudgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private JudgeHistoryRepository judgeHistoryRepository;

    @Autowired
    private JudgeFeedbackRepository judgeFeedbackRepository;

    @MockitoBean
    private JudgeAnalysisClient judgeAnalysisClient;

    private User sender;
    private User partner;

    @BeforeEach
    void setUp() {
        judgeFeedbackRepository.deleteAll();
        judgeHistoryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        userRepository.deleteAll();

        sender = userRepository.save(User.builder()
                .email("judge-sender@example.com")
                .password("encoded-password")
                .nickname("sender")
                .provider("LOCAL")
                .build());
        partner = userRepository.save(User.builder()
                .email("judge-partner@example.com")
                .password("encoded-password")
                .nickname("partner")
                .provider("LOCAL")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(sender.getUserId())
                .role("OWNER")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(partner.getUserId())
                .role("PARTNER")
                .build());
    }

    @Test
    void saveJudgeFeedbackStoresFeedback() throws Exception {
        JudgeHistory history = judgeHistoryRepository.save(judgeHistory(1L));

        mockMvc.perform(post("/api/v1/judge/{judgeHistoryId}/feedback", history.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfied": true,
                                  "feedbackText": "상대방 입장을 잘 설명해줘서 도움이 되었어요."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.judgeHistoryId").value(history.getId()))
                .andExpect(jsonPath("$.satisfied").value(true));

        assertThat(judgeFeedbackRepository.findAll()).hasSize(1);
        assertThat(judgeFeedbackRepository.findAll().get(0).getSatisfied()).isTrue();
        assertThat(judgeFeedbackRepository.findAll().get(0).getFeedbackText())
                .isEqualTo("상대방 입장을 잘 설명해줘서 도움이 되었어요.");
    }

    @Test
    void saveJudgeFeedbackUpdatesExistingFeedback() throws Exception {
        JudgeHistory history = judgeHistoryRepository.save(judgeHistory(1L));

        mockMvc.perform(post("/api/v1/judge/{judgeHistoryId}/feedback", history.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfied": true,
                                  "feedbackText": "도움이 되었어요."
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/judge/{judgeHistoryId}/feedback", history.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfied": false,
                                  "feedbackText": "조금 더 구체적이면 좋겠어요."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.satisfied").value(false));

        assertThat(judgeFeedbackRepository.findAll()).hasSize(1);
        JudgeFeedback feedback = judgeFeedbackRepository.findAll().get(0);
        assertThat(feedback.getSatisfied()).isFalse();
        assertThat(feedback.getFeedbackText()).isEqualTo("조금 더 구체적이면 좋겠어요.");
    }

    @Test
    void saveJudgeFeedbackRejectsOtherCouplesHistory() throws Exception {
        User outsider = userRepository.save(User.builder()
                .email("judge-outsider@example.com")
                .password("encoded-password")
                .nickname("outsider")
                .provider("LOCAL")
                .build());
        User outsiderPartner = userRepository.save(User.builder()
                .email("judge-outsider-partner@example.com")
                .password("encoded-password")
                .nickname("outsiderPartner")
                .provider("LOCAL")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(2L)
                .userId(outsider.getUserId())
                .role("OWNER")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(2L)
                .userId(outsiderPartner.getUserId())
                .role("PARTNER")
                .build());
        JudgeHistory history = judgeHistoryRepository.save(judgeHistory(2L));

        mockMvc.perform(post("/api/v1/judge/{judgeHistoryId}/feedback", history.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfied": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveJudgeFeedbackReturnsNotFoundForMissingHistory() throws Exception {
        mockMvc.perform(post("/api/v1/judge/{judgeHistoryId}/feedback", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfied": true
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistoriesIncludesCurrentUsersFeedback() throws Exception {
        JudgeHistory history = judgeHistoryRepository.save(judgeHistory(1L));
        judgeFeedbackRepository.save(JudgeFeedback.builder()
                .judgeHistory(history)
                .userId(sender.getUserId())
                .satisfied(true)
                .feedbackText("도움이 되었어요.")
                .build());

        mockMvc.perform(get("/api/v1/judge/histories")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(history.getId()))
                .andExpect(jsonPath("$[0].feedbackSubmitted").value(true))
                .andExpect(jsonPath("$[0].satisfied").value(true))
                .andExpect(jsonPath("$[0].feedbackText").value("도움이 되었어요."));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }

    private JudgeHistory judgeHistory(Long coupleId) {
        return JudgeHistory.builder()
                .coupleId(coupleId)
                .triggerMessageId(100L)
                .summaryA("A 입장")
                .summaryB("B 입장")
                .judgement("판결문")
                .solution("해결책")
                .reconciliationMessage("화해 메시지")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.WITTY)
                .build();
    }
}
