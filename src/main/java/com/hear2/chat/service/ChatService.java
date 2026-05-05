package com.hear2.chat.service;

import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.dto.ChatReadRequest;
import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        validateRequiredIds(request);

        MessageType messageType = validateMessageType(request.getMessageType());
        validateMessagePayload(request, messageType);

        ChatMessage message = ChatMessage.builder()
                .coupleId(request.getCoupleId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(normalizeContent(request.getContent(), messageType))
                .messageType(messageType)
                .mediaUrl(resolveMediaUrl(request, messageType))
                .originalFileName(resolveOriginalFileName(request, messageType))
                .mediaContentType(resolveMediaContentType(request, messageType))
                .mediaSize(resolveMediaSize(request, messageType))
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long coupleId) {
        return chatMessageRepository.findByCoupleIdOrderByCreatedAtAsc(coupleId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatReadResponse markMessagesAsRead(ChatReadRequest request) {
        validateReadRequest(request);

        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(
                        request.getCoupleId(),
                        request.getReaderId()
                );

        LocalDateTime readAt = LocalDateTime.now();
        unreadMessages.forEach(message -> message.markAsRead(readAt));

        return ChatReadResponse.builder()
                .coupleId(request.getCoupleId())
                .readerId(request.getReaderId())
                .readMessageIds(unreadMessages.stream()
                        .map(ChatMessage::getId)
                        .toList())
                .readAt(readAt)
                .build();
    }

    private void validateRequiredIds(ChatMessageRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message request is required");
        }
        if (request.getCoupleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
        if (request.getSenderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderId is required");
        }
        if (request.getReceiverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverId is required");
        }
        if (request.getSenderId().equals(request.getReceiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderId and receiverId must be different");
        }
    }

    private void validateReadRequest(ChatReadRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "read request is required");
        }
        if (request.getCoupleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
        if (request.getReaderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "readerId is required");
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
            if (StringUtils.hasText(request.getMediaUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEXT messages must not include mediaUrl");
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
