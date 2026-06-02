package com.hear2.emotion.service;

import com.hear2.chat.dto.EmotionFeedbackRequest;
import com.hear2.chat.dto.EmotionFeedbackResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.entity.EmotionAnalysisFeedback;
import com.hear2.emotion.repository.EmotionAnalysisFeedbackRepository;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmotionFeedbackService {

    private final ChatParticipantResolver chatParticipantResolver;
    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final EmotionAnalysisFeedbackRepository emotionAnalysisFeedbackRepository;

    @Transactional
    public EmotionFeedbackResponse saveFeedback(Long currentUserId, Long messageId, EmotionFeedbackRequest request) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }
        if (request == null || request.getIsCorrect() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isCorrect is required");
        }

        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));
        if (!context.coupleId().equals(message.getCoupleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "message does not belong to your couple");
        }
        if (message.getMessageType() != MessageType.TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emotion feedback is only available for TEXT messages");
        }

        EmotionAnalysis analysis = emotionAnalysisRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "emotion analysis not found"));

        EmotionAnalysisFeedback feedback = emotionAnalysisFeedbackRepository.findByMessageIdAndUserId(messageId, currentUserId)
                .map(existing -> {
                    existing.update(request.getIsCorrect(), analysis);
                    return existing;
                })
                .orElseGet(() -> EmotionAnalysisFeedback.builder()
                        .message(message)
                        .analysis(analysis)
                        .userId(currentUserId)
                        .isCorrect(request.getIsCorrect())
                        .build());

        EmotionAnalysisFeedback saved = emotionAnalysisFeedbackRepository.save(feedback);
        return EmotionFeedbackResponse.builder()
                .messageId(saved.getMessage().getId())
                .isCorrect(saved.getIsCorrect())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> findFeedbackByMessageIdsAndUserId(List<Long> messageIds, Long userId) {
        if (userId == null || messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }

        return emotionAnalysisFeedbackRepository.findByMessageIdInAndUserId(messageIds, userId).stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getMessage().getId(),
                        EmotionAnalysisFeedback::getIsCorrect
                ));
    }
}
