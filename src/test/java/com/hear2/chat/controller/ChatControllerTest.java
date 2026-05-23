package com.hear2.chat.controller;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.service.CharacterService;
import com.hear2.character.support.CharacterExpSourceType;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.service.EmotionAnalysisService;
import com.hear2.global.security.JwtProvider;
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

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private EmotionAnalysisService emotionAnalysisService;

    @MockitoBean
    private CharacterService characterService;

    @MockitoBean
    private CharacterExpHistoryRepository characterExpHistoryRepository;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        reset(emotionAnalysisService, characterService, characterExpHistoryRepository);
        chatMessageRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        userRepository.deleteAll();

        sender = userRepository.save(User.builder()
                .email("chat-sender@example.com")
                .password("encoded-password")
                .nickname("sender")
                .provider("LOCAL")
                .build());
        receiver = userRepository.save(User.builder()
                .email("chat-receiver@example.com")
                .password("encoded-password")
                .nickname("receiver")
                .provider("LOCAL")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(sender.getUserId())
                .role("OWNER")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(1L)
                .userId(receiver.getUserId())
                .role("PARTNER")
                .build());

        when(characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
                eq(1L), any(), eq(CharacterExpSourceType.CHAT)
        )).thenReturn(0L);
    }

    @Test
    void sendMessageReturnsEmotionFieldsImmediatelyWhenAnalysisSucceeds() throws Exception {
        when(emotionAnalysisService.analyzeAndSave(any(ChatMessage.class)))
                .thenReturn(EmotionAnalysisResponse.builder()
                        .emotionType(EmotionType.HAPPY)
                        .emotionScore(0.95)
                        .negativeScore(0.05)
                        .emotionEmoji("😊")
                        .riskLevel(RiskLevel.WARNING)
                        .riskDetected(true)
                        .riskReason("risk")
                        .build());

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "오늘 너무 좋아",
                                  "messageType": "TEXT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("오늘 너무 좋아"))
                .andExpect(jsonPath("$.emotionEmoji").value("😊"))
                .andExpect(jsonPath("$.emotionType").value("HAPPY"))
                .andExpect(jsonPath("$.riskLevel").value("WARNING"))
                .andExpect(jsonPath("$.judgeAvailable").value(true));

        assertThat(chatMessageRepository.countByCoupleId(1L)).isEqualTo(1L);
    }

    @Test
    void sendMessageStillSucceedsWhenEmotionAnalysisFails() throws Exception {
        doThrow(new RuntimeException("AI down"))
                .when(emotionAnalysisService).analyzeAndSave(any(ChatMessage.class));

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "분석 실패여도 저장",
                                  "messageType": "TEXT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("분석 실패여도 저장"))
                .andExpect(jsonPath("$.emotionEmoji").value(nullValue()))
                .andExpect(jsonPath("$.emotionType").value(nullValue()))
                .andExpect(jsonPath("$.judgeAvailable").value(false));

        assertThat(chatMessageRepository.countByCoupleId(1L)).isEqualTo(1L);
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
