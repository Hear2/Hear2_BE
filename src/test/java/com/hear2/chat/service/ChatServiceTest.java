package com.hear2.chat.service;

import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.service.CharacterService;
import com.hear2.character.support.CharacterExpSourceType;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.service.EmotionFeedbackService;
import com.hear2.emotion.service.EmotionAnalysisService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisService emotionAnalysisService = mock(EmotionAnalysisService.class);
    private final EmotionFeedbackService emotionFeedbackService = mock(EmotionFeedbackService.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final CharacterService characterService = mock(CharacterService.class);
    private final CharacterExpHistoryRepository characterExpHistoryRepository = mock(CharacterExpHistoryRepository.class);
    private final ChatService chatService = new ChatService(
            chatMessageRepository,
            emotionAnalysisService,
            emotionFeedbackService,
            chatParticipantResolver,
            characterService,
            characterExpHistoryRepository
    );

    @Test
    void savesTextMessageAndReturnsEmotionWhenAnalysisSucceeds() throws Exception {
        ChatMessageRequest request = textRequest("안녕");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 100L));
        when(characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
                eq(1L), any(LocalDate.class), eq(CharacterExpSourceType.CHAT)
        )).thenReturn(0L);
        when(emotionAnalysisService.analyzeAndSave(any(ChatMessage.class)))
                .thenReturn(EmotionAnalysisResponse.builder()
                        .emotionType(EmotionType.HAPPY)
                        .emotionScore(0.91)
                        .emotionEmoji("😊")
                        .build());

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("안녕");
        assertThat(response.getEmotionType()).isEqualTo(EmotionType.HAPPY);
        assertThat(response.getEmotionEmoji()).isEqualTo("😊");
        verify(characterService).grantExp(1L, CharacterExpSourceType.CHAT, "CHAT:100", 1L);
    }

    @Test
    void savesTextMessageEvenWhenEmotionAnalysisFails() throws Exception {
        ChatMessageRequest request = textRequest("안녕");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 101L));
        when(characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
                eq(1L), any(LocalDate.class), eq(CharacterExpSourceType.CHAT)
        )).thenReturn(0L);
        doThrow(new RuntimeException("AI server down"))
                .when(emotionAnalysisService).analyzeAndSave(any(ChatMessage.class));

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getContent()).isEqualTo("안녕");
        assertThat(response.getEmotionType()).isNull();
        assertThat(response.getEmotionEmoji()).isNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(characterService).grantExp(1L, CharacterExpSourceType.CHAT, "CHAT:101", 1L);
    }

    @Test
    void savesImageMessageWithoutEmotionAnalysis() throws Exception {
        ChatMessageRequest request = imageRequest("https://example.com/image.png");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 102L));

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(102L);
        assertThat(response.getMessageType()).isEqualTo(MessageType.IMAGE);
        verify(emotionAnalysisService, never()).analyzeAndSave(any());
        verify(characterService, never()).grantExp(anyLong(), any(), any(), anyLong());
    }

    @Test
    void savesTextMessageWithoutGrantingExpWhenDailyChatLimitReached() throws Exception {
        ChatMessageRequest request = textRequest("hello");
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> savedMessage(invocation.getArgument(0), 151L));
        when(characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
                eq(1L), any(LocalDate.class), eq(CharacterExpSourceType.CHAT)
        )).thenReturn(50L);

        ChatMessageResponse response = chatService.saveMessage(10L, request);

        assertThat(response.getId()).isEqualTo(151L);
        verify(characterService, never()).grantExp(any(), any(), any(), eq(1L));
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

    @Test
    void getMessagesIncludesCurrentUsersEmotionFeedback() {
        ChatMessage first = message(100L);
        ChatMessage second = message(101L);
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.HAPPY)
                .emotionEmoji("😊")
                .build();

        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        when(chatMessageRepository.findByCoupleIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(first, second));
        when(emotionAnalysisService.findByMessageIds(List.of(100L, 101L)))
                .thenReturn(java.util.Map.of(100L, emotion));
        when(emotionFeedbackService.findFeedbackByMessageIdsAndUserId(List.of(100L, 101L), 10L))
                .thenReturn(java.util.Map.of(100L, true, 101L, false));

        List<ChatMessageResponse> responses = chatService.getMessages(10L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmotionEmoji()).isEqualTo("😊");
        assertThat(responses.get(0).getEmotionFeedback()).isTrue();
        assertThat(responses.get(1).getEmotionFeedback()).isFalse();
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
