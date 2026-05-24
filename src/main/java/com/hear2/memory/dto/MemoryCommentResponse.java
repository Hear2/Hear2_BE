package com.hear2.memory.dto;

import com.hear2.memory.entity.MemoryComment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MemoryCommentResponse(
        Long id,
        Long memoryId,
        Long writerId,
        String writerNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemoryCommentResponse from(MemoryComment comment, String writerNickname) {
        return MemoryCommentResponse.builder()
                .id(comment.getId())
                .memoryId(comment.getMemoryId())
                .writerId(comment.getWriterId())
                .writerNickname(writerNickname)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
