package com.hear2.judge.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.client.JudgeAnalysisClient;
import com.hear2.judge.dto.JudgeFastApiRequest;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisRepository emotionAnalysisRepository = mock(EmotionAnalysisRepository.class);
    private final JudgeHistoryRepository judgeHistoryRepository = mock(JudgeHistoryRepository.class);
    private final JudgeAnalysisClient judgeAnalysisClient = mock(JudgeAnalysisClient.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final JudgeService judgeService = new JudgeService(
            chatMessageRepository,
            emotionAnalysisRepository,
            judgeHistoryRepository,
            judgeAnalysisClient,
            chatParticipantResolver
    );

    @Test
    void judgesUsingLoggedInUsersCoupleAndUserId() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        ChatMessage triggerMessage = message(100L, 1L, 10L, 11L);
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(triggerMessage));
        when(emotionAnalysisRepository.findByMessageId(100L)).thenReturn(Optional.of(emotion(triggerMessage)));
        when(chatMessageRepository.findTop20ByCoupleIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(triggerMessage));
        when(emotionAnalysisRepository.findByMessageIdIn(List.of(100L)))
                .thenReturn(List.of(emotion(triggerMessage)));
        when(judgeAnalysisClient.requestJudgement(any(JudgeFastApiRequest.class)))
                .thenReturn(judgeResponse());
        when(judgeHistoryRepository.save(any(JudgeHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(judgeHistoryRepository.countByCoupleIdAndConflictType(1L, ConflictType.COMMUNICATION))
                .thenReturn(1L);

        JudgeRequest request = new JudgeRequest();
        request.setTriggerMessageId(100L);

        judgeService.judge(10L, request);

        ArgumentCaptor<JudgeFastApiRequest> captor = ArgumentCaptor.forClass(JudgeFastApiRequest.class);
        verify(judgeAnalysisClient).requestJudgement(captor.capture());
        assertThat(captor.getValue().getCoupleId()).isEqualTo(1L);
        assertThat(captor.getValue().getRequestedByUserId()).isEqualTo(10L);
    }

    @Test
    void rejectsTriggerMessageFromAnotherCouple() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findById(100L))
                .thenReturn(Optional.of(message(100L, 2L, 20L, 21L)));
        JudgeRequest request = new JudgeRequest();
        request.setTriggerMessageId(100L);

        assertThatThrownBy(() -> judgeService.judge(10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("trigger message does not belong to your couple");
    }

    private ChatMessage message(Long id, Long coupleId, Long senderId, Long receiverId) {
        return ChatMessage.builder()
                .id(id)
                .coupleId(coupleId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content("Please listen to me")
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private EmotionAnalysis emotion(ChatMessage message) {
        return EmotionAnalysis.builder()
                .message(message)
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.9)
                .negativeScore(0.8)
                .riskLevel(RiskLevel.WARNING)
                .riskDetected(true)
                .build();
    }

    private JudgeResponse judgeResponse() {
        return JudgeResponse.builder()
                .summaryA("A")
                .summaryB("B")
                .judgement("Judgement")
                .solution("Solution")
                .reconciliationMessage("Message")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.WITTY)
                .build();
    }
}
