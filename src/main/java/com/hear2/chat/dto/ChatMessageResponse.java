package com.hear2.chat.dto;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long coupleId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private MessageType messageType;
    private String mediaUrl;
    private String originalFileName;
    private String mediaContentType;
    private Long mediaSize;
    private LocalDateTime readAt;
    private Integer unreadCount;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .coupleId(message.getCoupleId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .mediaUrl(message.getMediaUrl())
                .originalFileName(message.getOriginalFileName())
                .mediaContentType(message.getMediaContentType())
                .mediaSize(message.getMediaSize())
                .readAt(message.getReadAt())
                .unreadCount(message.getReadAt() == null ? 1 : 0)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
