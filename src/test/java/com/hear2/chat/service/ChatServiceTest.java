package com.hear2.chat.service;

import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.service.EmotionAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisService emotionAnalysisService = mock(EmotionAnalysisService.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final ChatService chatService = new ChatService(
            chatMessageRepository,
            emotionAnalysisService,
            chatParticipantResolver
    );

    @Test
    void marksMessagesReceivedByLoggedInUserAsRead() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(1L, 10L))
                .thenReturn(List.of(message(100L)));

        ChatReadResponse response = chatService.markMessagesAsRead(10L);

        assertThat(response.getCoupleId()).isEqualTo(1L);
        assertThat(response.getReaderId()).isEqualTo(10L);
        assertThat(response.getReadMessageIds()).containsExactly(100L);
        verify(chatMessageRepository)
                .findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(1L, 10L);
    }

    private ChatMessage message(Long id) {
        return ChatMessage.builder()
                .id(id)
                .coupleId(1L)
                .senderId(11L)
                .receiverId(10L)
                .content("message to mark as read")
                .messageType(MessageType.TEXT)
                .build();
    }
}
