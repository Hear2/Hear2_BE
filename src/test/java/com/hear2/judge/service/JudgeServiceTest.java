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
import com.hear2.judge.dto.JudgeFeedbackSummary;
import com.hear2.judge.dto.JudgeHistoryResponse;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
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
    private final JudgeFeedbackService judgeFeedbackService = mock(JudgeFeedbackService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JudgeService judgeService = new JudgeService(
            chatMessageRepository,
            emotionAnalysisRepository,
            judgeHistoryRepository,
            judgeAnalysisClient,
            chatParticipantResolver,
            judgeFeedbackService,
            userRepository
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
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(
                        user(10L, "승현"),
                        user(11L, "지민")
                ));

        JudgeRequest request = new JudgeRequest();
        request.setTriggerMessageId(100L);

        JudgeResponse response = judgeService.judge(10L, request);

        ArgumentCaptor<JudgeFastApiRequest> captor = ArgumentCaptor.forClass(JudgeFastApiRequest.class);
        verify(judgeAnalysisClient).requestJudgement(captor.capture());
        assertThat(captor.getValue().getCoupleId()).isEqualTo(1L);
        assertThat(captor.getValue().getRequestedByUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getPartnerUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getRequestedByName()).isEqualTo("승현");
        assertThat(captor.getValue().getPartnerName()).isEqualTo("지민");
        assertThat(response.getSummaryA()).isEqualTo("승현은 상대의 말을 듣고 싶었습니다.");
        assertThat(response.getSummaryB()).isEqualTo("지민은 충분히 설명했다고 느꼈습니다.");
        assertThat(response.getReconciliationMessage()).isEqualTo("내 말이 날카로웠어. 왜 서운했는지 차분히 다시 듣고 싶어.");
        assertThat(response.getRequestedReconciliationMessage()).isEqualTo("내 말이 날카로웠어. 왜 서운했는지 차분히 다시 듣고 싶어.");
        assertThat(response.getPartnerReconciliationMessage()).isEqualTo("내가 바로 설명만 하려 했네. 어떤 지점이 가장 아팠는지 먼저 듣고 싶어.");
        assertThat(response.getUserName()).isEqualTo("승현");
        assertThat(response.getPartnerName()).isEqualTo("지민");
        assertThat(response.getFeedbackSubmitted()).isFalse();
        assertThat(response.getSatisfied()).isNull();

        ArgumentCaptor<JudgeHistory> historyCaptor = ArgumentCaptor.forClass(JudgeHistory.class);
        verify(judgeHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRequestedByUserId()).isEqualTo(10L);
        assertThat(historyCaptor.getValue().getPartnerUserId()).isEqualTo(11L);
        assertThat(historyCaptor.getValue().getSummaryA()).isEqualTo("상대의 말을 듣고 싶었습니다.");
        assertThat(historyCaptor.getValue().getSummaryB()).isEqualTo("충분히 설명했다고 느꼈습니다.");
        assertThat(historyCaptor.getValue().getRequestedReconciliationMessage())
                .isEqualTo("내 말이 날카로웠어. 왜 서운했는지 차분히 다시 듣고 싶어.");
        assertThat(historyCaptor.getValue().getPartnerReconciliationMessage())
                .isEqualTo("내가 바로 설명만 하려 했네. 어떤 지점이 가장 아팠는지 먼저 듣고 싶어.");
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

    @Test
    void getHistoriesIncludesCurrentUsersFeedback() {
        JudgeHistory history = JudgeHistory.builder()
                .id(7L)
                .coupleId(1L)
                .requestedByUserId(10L)
                .partnerUserId(11L)
                .summaryA("서운했습니다.")
                .summaryB("충분히 설명했다고 느꼈습니다.")
                .judgement("judgement")
                .requestedReconciliationMessage("내가 먼저 차분히 말해볼게.")
                .partnerReconciliationMessage("내가 먼저 감정을 인정해볼게.")
                .createdAt(LocalDateTime.now())
                .build();
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(judgeHistoryRepository.findByCoupleIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(history));
        when(judgeFeedbackService.findFeedbackByJudgeHistoryIdsAndUserId(List.of(7L), 10L))
                .thenReturn(java.util.Map.of(7L, JudgeFeedbackSummary.builder()
                        .feedbackSubmitted(true)
                        .satisfied(true)
                        .feedbackText("helpful")
                        .build()));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(
                        user(10L, "민수"),
                        user(11L, "지연")
                ));

        List<JudgeHistoryResponse> responses = judgeService.getHistories(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSummaryA()).isEqualTo("민수는 서운했습니다.");
        assertThat(responses.get(0).getSummaryB()).isEqualTo("지연은 충분히 설명했다고 느꼈습니다.");
        assertThat(responses.get(0).getReconciliationMessage()).isEqualTo("내가 먼저 차분히 말해볼게.");
        assertThat(responses.get(0).getRequestedReconciliationMessage()).isEqualTo("내가 먼저 차분히 말해볼게.");
        assertThat(responses.get(0).getPartnerReconciliationMessage()).isEqualTo("내가 먼저 감정을 인정해볼게.");
        assertThat(responses.get(0).getUserName()).isEqualTo("민수");
        assertThat(responses.get(0).getPartnerName()).isEqualTo("지연");
        assertThat(responses.get(0).getFeedbackSubmitted()).isTrue();
        assertThat(responses.get(0).getSatisfied()).isTrue();
        assertThat(responses.get(0).getFeedbackText()).isEqualTo("helpful");
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
                .summaryA("승현은 상대의 말을 듣고 싶었습니다.")
                .summaryB("지민은 충분히 설명했다고 느꼈습니다.")
                .judgement("Judgement")
                .solution("Solution")
                .requestedReconciliationMessage("내 말이 날카로웠어. 왜 서운했는지 차분히 다시 듣고 싶어.")
                .partnerReconciliationMessage("내가 바로 설명만 하려 했네. 어떤 지점이 가장 아팠는지 먼저 듣고 싶어.")
                .conflictType(ConflictType.COMMUNICATION)
                .judgeTone(JudgeTone.WITTY)
                .build();
    }

    private User user(Long id, String nickname) {
        return User.builder()
                .userId(id)
                .nickname(nickname)
                .build();
    }
}
