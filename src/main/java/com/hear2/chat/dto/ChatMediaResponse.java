package com.hear2.chat.dto;

import lombok.Builder;
import lombok.Getter;

import com.hear2.chat.entity.MessageType;

@Getter
@Builder
public class ChatMediaResponse {

    private MessageType messageType;
    private String mediaUrl;
    private String originalFileName;
    private String mediaContentType;
    private Long mediaSize;
}
