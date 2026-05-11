package com.hear2.emotion.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.emotion.client.EmotionAnalysisClient;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.dto.RiskAnalysisResponse;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.notification.service.FcmNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmotionAnalysisServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisRepository emotionAnalysisRepository = mock(EmotionAnalysisRepository.class);
    private final EmotionAnalysisClient emotionAnalysisClient = mock(EmotionAnalysisClient.class);
    private final RiskDetectionService riskDetectionService = mock(RiskDetectionService.class);
    private final FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final EmotionAnalysisService emotionAnalysisService = new EmotionAnalysisService(
            chatMessageRepository,
            emotionAnalysisRepository,
            emotionAnalysisClient,
            riskDetectionService,
            fcmNotificationService,
            chatParticipantResolver
    );

    @Test
    void returnsExistingAnalysisOnlyForLoggedInUsersCoupleMessage() {
        ChatMessage message = message(100L, 1L, 10L, 11L);
        EmotionAnalysis analysis = EmotionAnalysis.builder()
                .message(message)
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.91)
                .negativeScore(0.83)
                .emotionEmoji("😡")
                .riskLevel(RiskLevel.WARNING)
                .riskDetected(true)
                .riskReason("risk")
                .detectedRiskKeywords("짜증")
                .build();

        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(message));
        when(emotionAnalysisRepository.findByMessageId(100L)).thenReturn(Optional.of(analysis));

        EmotionAnalysisResponse response = emotionAnalysisService.analyzeMessageForUser(10L, 100L);

        assertThat(response.getEmotionType()).isEqualTo(EmotionType.ANGRY);
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.WARNING);
        verify(emotionAnalysisClient, never()).analyze(any(), any());
    }

    @Test
    void rejectsMessageOutsideLoggedInUsersCouple() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findById(100L))
                .thenReturn(Optional.of(message(100L, 2L, 20L, 21L)));

        assertThatThrownBy(() -> emotionAnalysisService.analyzeMessageForUser(10L, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("message does not belong to your couple");
    }

    @Test
    void analyzesAndSavesWhenNoExistingAnalysisExists() {
        ChatMessage message = message(100L, 1L, 10L, 11L);
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(message));
        when(emotionAnalysisRepository.findByMessageId(100L)).thenReturn(Optional.empty());
        when(emotionAnalysisClient.analyze(any(), any())).thenReturn(emotionResponse());
        when(riskDetectionService.analyze(any(), any())).thenReturn(RiskAnalysisResponse.builder()
                .riskLevel(RiskLevel.WARNING)
                .riskDetected(true)
                .riskReason("risk")
                .detectedRiskKeywords(List.of("짜증"))
                .build());

        EmotionAnalysisResponse response = emotionAnalysisService.analyzeMessageForUser(10L, 100L);

        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.WARNING);
        verify(emotionAnalysisRepository).save(any(EmotionAnalysis.class));
        verify(fcmNotificationService).sendRiskAlert(any(ChatMessage.class), any(EmotionAnalysisResponse.class));
    }

    private ChatMessage message(Long id, Long coupleId, Long senderId, Long receiverId) {
        return ChatMessage.builder()
                .id(id)
                .coupleId(coupleId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content("안녕")
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private EmotionAnalysisResponse emotionResponse() {
        return EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.9)
                .negativeScore(0.8)
                .emotionEmoji("😡")
                .build();
    }
}
