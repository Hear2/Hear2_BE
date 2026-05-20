package com.hear2.chat.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEmotionAnalysisListener {

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisService emotionAnalysisService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void analyze(ChatMessageSavedEvent event) {
        try {
            ChatMessage message = chatMessageRepository.findById(event.messageId())
                    .orElse(null);
            if (message == null) {
                log.warn("Skipping emotion analysis because message was not found. messageId={}", event.messageId());
                return;
            }

            emotionAnalysisService.analyzeAndSave(message);
        } catch (Exception exception) {
            log.warn("Emotion analysis failed after message save. messageId={}", event.messageId(), exception);
        }
    }
}
