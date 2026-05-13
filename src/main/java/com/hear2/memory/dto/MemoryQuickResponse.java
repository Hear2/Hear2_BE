package com.hear2.memory.dto;

import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryTagSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Builder
@Schema(description = "3초 기록 응답")
public class MemoryQuickResponse {

    private Long id;
    private String imageUrl;
    private String aiPlace;
    private String aiTime;
    private List<String> aiTags;
    private List<String> userTags;
    private String note;

    public static MemoryQuickResponse from(Memory memory) {
        return MemoryQuickResponse.builder()
                .id(memory.getId())
                .imageUrl(MemoryResponse.resolvePhotoUrl(memory))
                .aiPlace(memory.getPhotoMetadata() == null ? null : memory.getPhotoMetadata().getLocationName())
                .aiTime(resolveAiTime(memory))
                .aiTags(memory.getAiTags().stream()
                        .filter(tag -> tag.getSource() == MemoryTagSource.AI)
                        .map(tag -> "#" + tag.getTagName())
                        .toList())
                .userTags(memory.getAiTags().stream()
                        .filter(tag -> tag.getSource() == MemoryTagSource.USER)
                        .map(tag -> "#" + tag.getTagName())
                        .toList())
                .note(memory.getMemo())
                .build();
    }

    private static String resolveAiTime(Memory memory) {
        if (memory.getPhotoMetadata() == null || memory.getPhotoMetadata().getTakenAt() == null) {
            return null;
        }

        return memory.getPhotoMetadata().getTakenAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
