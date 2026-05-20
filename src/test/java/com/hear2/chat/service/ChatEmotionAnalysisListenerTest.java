package com.hear2.chat.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.service.EmotionAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatEmotionAnalysisListenerTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final EmotionAnalysisService emotionAnalysisService = mock(EmotionAnalysisService.class);
    private final ChatEmotionAnalysisListener listener = new ChatEmotionAnalysisListener(
            chatMessageRepository,
            emotionAnalysisService
    );

    @Test
    void analyzesSavedMessageWhenItExists() {
        ChatMessage message = ChatMessage.builder()
                .id(100L)
                .coupleId(1L)
                .senderId(10L)
                .receiverId(11L)
                .content("안녕")
                .messageType(MessageType.TEXT)
                .build();
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(message));

        listener.analyze(new ChatMessageSavedEvent(100L));

        verify(emotionAnalysisService).analyzeAndSave(message);
    }

    @Test
    void ignoresMissingMessage() {
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.empty());

        listener.analyze(new ChatMessageSavedEvent(100L));

        verifyNoInteractions(emotionAnalysisService);
    }

    @Test
    void swallowsEmotionAnalysisFailure() {
        ChatMessage message = ChatMessage.builder()
                .id(100L)
                .coupleId(1L)
                .senderId(10L)
                .receiverId(11L)
                .content("안녕")
                .messageType(MessageType.TEXT)
                .build();
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(message));
        doThrow(new RuntimeException("AI down")).when(emotionAnalysisService).analyzeAndSave(message);

        listener.analyze(new ChatMessageSavedEvent(100L));

        verify(emotionAnalysisService).analyzeAndSave(message);
    }
}
