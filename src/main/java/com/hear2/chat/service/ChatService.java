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
import com.hear2.emotion.service.EmotionFeedbackService;
import com.hear2.emotion.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final long CHAT_EXP = 1L;
    private static final long DAILY_CHAT_EXP_LIMIT = 50L;

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisService emotionAnalysisService;
    private final EmotionFeedbackService emotionFeedbackService;
    private final ChatParticipantResolver chatParticipantResolver;
    private final CharacterService characterService;
    private final CharacterExpHistoryRepository characterExpHistoryRepository;

    @Transactional
    public ChatMessageResponse saveMessage(Long currentUserId, ChatMessageRequest request) {
        validateRequest(request);

        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);

        MessageType messageType = validateMessageType(request.getMessageType());
        validateMessagePayload(request, messageType);

        ChatMessage message = ChatMessage.builder()
                .coupleId(context.coupleId())
                .senderId(context.senderId())
                .receiverId(context.receiverId())
                .content(normalizeContent(request.getContent(), messageType))
                .messageType(messageType)
                .mediaUrl(resolveMediaUrl(request, messageType))
                .originalFileName(resolveOriginalFileName(request, messageType))
                .mediaContentType(resolveMediaContentType(request, messageType))
                .mediaSize(resolveMediaSize(request, messageType))
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        EmotionAnalysisResponse emotion = null;
        if (messageType == MessageType.TEXT) {
            grantChatExp(context.coupleId(), savedMessage.getId());
            try {
                emotion = emotionAnalysisService.analyzeAndSave(savedMessage);
            } catch (Exception exception) {
                log.warn("Emotion analysis failed after chat message save. messageId={}", savedMessage.getId(), exception);
            }
        }

        return ChatMessageResponse.from(savedMessage, emotion);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long currentUserId) {
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);

        List<ChatMessage> messages = chatMessageRepository.findByCoupleIdOrderByCreatedAtAsc(context.coupleId());
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();
        Map<Long, EmotionAnalysisResponse> emotionsByMessageId = emotionAnalysisService.findByMessageIds(messageIds);
        Map<Long, Boolean> feedbackByMessageId = emotionFeedbackService.findFeedbackByMessageIdsAndUserId(messageIds, currentUserId);

        return messages.stream()
                .map(message -> ChatMessageResponse.from(
                        message,
                        emotionsByMessageId.get(message.getId()),
                        feedbackByMessageId.get(message.getId())
                ))
                .toList();
    }

    @Transactional
    public ChatReadResponse markMessagesAsRead(Long currentUserId) {
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);

        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(
                        context.coupleId(),
                        context.senderId()
                );

        LocalDateTime readAt = LocalDateTime.now();
        unreadMessages.forEach(message -> message.markAsRead(readAt));

        return ChatReadResponse.builder()
                .coupleId(context.coupleId())
                .readerId(context.senderId())
                .readMessageIds(unreadMessages.stream()
                        .map(ChatMessage::getId)
                        .toList())
                .readAt(readAt)
                .build();
    }

    private void validateRequest(ChatMessageRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message request is required");
        }
    }

    private MessageType validateMessageType(MessageType messageType) {
        if (messageType == null) {
            return MessageType.TEXT;
        }
        return messageType;
    }

    private void validateMessagePayload(ChatMessageRequest request, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            if (!StringUtils.hasText(request.getContent())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required for TEXT messages");
            }
            return;
        }

        if (!StringUtils.hasText(request.getMediaUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaUrl is required for media messages");
        }
        if (!isSupportedMediaUrl(request.getMediaUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaUrl must be an uploaded chat media URL");
        }
    }

    private boolean isSupportedMediaUrl(String mediaUrl) {
        return mediaUrl.startsWith("/uploads/")
                || mediaUrl.startsWith("http://")
                || mediaUrl.startsWith("https://");
    }

    private String normalizeContent(String content, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            return content.trim();
        }
        return content == null ? "" : content;
    }

    private String resolveMediaUrl(ChatMessageRequest request, MessageType messageType) {
        return messageType == MessageType.TEXT ? null : request.getMediaUrl();
    }

    private String resolveOriginalFileName(ChatMessageRequest request, MessageType messageType) {
        return messageType == MessageType.TEXT ? null : request.getOriginalFileName();
    }

    private String resolveMediaContentType(ChatMessageRequest request, MessageType messageType) {
        return messageType == MessageType.TEXT ? null : request.getMediaContentType();
    }

    private Long resolveMediaSize(ChatMessageRequest request, MessageType messageType) {
        return messageType == MessageType.TEXT ? null : request.getMediaSize();
    }

    private void grantChatExp(Long coupleId, Long chatMessageId) {
        long chatExpToday = characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
                coupleId,
                LocalDate.now(),
                CharacterExpSourceType.CHAT
        );
        long remainingChatExpToday = Math.max(DAILY_CHAT_EXP_LIMIT - chatExpToday, 0L);
        long requestedExp = Math.min(CHAT_EXP, remainingChatExpToday);
        if (requestedExp <= 0) {
            return;
        }

        characterService.grantExp(
                coupleId,
                CharacterExpSourceType.CHAT,
                "CHAT:" + chatMessageId,
                requestedExp
        );
    }
}
