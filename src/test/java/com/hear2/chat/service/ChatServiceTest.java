package com.hear2.chat.service;

import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.service.EmotionAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisService emotionAnalysisService = mock(EmotionAnalysisService.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ChatService chatService = new ChatService(
            chatMessageRepository,
            emotionAnalysisService,
            chatParticipantResolver,
            eventPublisher
    );

    @Test
    void savesTextMessageAndPublishesEmotionAnalysisEvent() throws Exception {
        ChatMessageRequest request = textRequest("안녕");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 100L));

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("안녕");
        assertThat(response.getEmotionType()).isNull();
        verify(eventPublisher).publishEvent(new ChatMessageSavedEvent(100L));
    }

    @Test
    void savesImageMessageWithoutPublishingEmotionAnalysisEvent() throws Exception {
        ChatMessageRequest request = imageRequest("https://example.com/image.png");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 101L));

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getMessageType()).isEqualTo(MessageType.IMAGE);
        verify(eventPublisher, never()).publishEvent(any());
    }

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

    private ChatMessageRequest textRequest(String content) throws Exception {
        ChatMessageRequest request = new ChatMessageRequest();
        setField(request, "content", content);
        setField(request, "messageType", MessageType.TEXT);
        return request;
    }

    private ChatMessageRequest imageRequest(String mediaUrl) throws Exception {
        ChatMessageRequest request = new ChatMessageRequest();
        setField(request, "messageType", MessageType.IMAGE);
        setField(request, "mediaUrl", mediaUrl);
        return request;
    }

    private ChatMessage savedMessage(ChatMessage original, Long id) {
        return ChatMessage.builder()
                .id(id)
                .coupleId(original.getCoupleId())
                .senderId(original.getSenderId())
                .receiverId(original.getReceiverId())
                .content(original.getContent())
                .messageType(original.getMessageType())
                .mediaUrl(original.getMediaUrl())
                .originalFileName(original.getOriginalFileName())
                .mediaContentType(original.getMediaContentType())
                .mediaSize(original.getMediaSize())
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
