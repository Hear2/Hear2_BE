package com.hear2.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatReadResponse {

    private Long coupleId;
    private Long readerId;
    private List<Long> readMessageIds;
    private LocalDateTime readAt;
}
