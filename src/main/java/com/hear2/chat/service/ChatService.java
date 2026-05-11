package com.hear2.chat.service;

import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisService emotionAnalysisService;
    private final ChatParticipantResolver chatParticipantResolver;

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

        EmotionAnalysisResponse emotion = messageType == MessageType.TEXT
                ? emotionAnalysisService.analyzeAndSave(savedMessage)
                : null;

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

        return messages.stream()
                .map(message -> ChatMessageResponse.from(message, emotionsByMessageId.get(message.getId())))
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
}
