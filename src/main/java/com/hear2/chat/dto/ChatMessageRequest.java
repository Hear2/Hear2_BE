package com.hear2.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.hear2.chat.entity.MessageType;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {

    private Long coupleId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private MessageType messageType;
    private String mediaUrl;
    private String originalFileName;
    private String mediaContentType;
    private Long mediaSize;
}
