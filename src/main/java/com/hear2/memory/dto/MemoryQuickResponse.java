package com.hear2.memory.dto;

import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryTagSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@Schema(description = "3초 기록 응답")
public class MemoryQuickResponse {

    private Long id;
    private String imageUrl;
    private List<String> imageUrls;
    private List<MemoryPhotoResponse> photos;
    private String aiPlace;
    private String aiTime;
    private List<String> aiTags;
    private List<String> userTags;
    private String note;

    public static MemoryQuickResponse from(Memory memory) {
        return from(memory, storedPhotoPath -> MemoryResponse.resolvePhotoUrl(storedPhotoPath, memory.getId()));
    }

    public static MemoryQuickResponse from(Memory memory, Function<String, String> photoUrlResolver) {
        MemoryResponse memoryResponse = MemoryResponse.from(memory, photoUrlResolver);

        return MemoryQuickResponse.builder()
                .id(memory.getId())
                .imageUrl(memoryResponse.getPhotoUrl())
                .imageUrls(memoryResponse.getPhotos().stream()
                        .map(MemoryPhotoResponse::getUrl)
                        .toList())
                .photos(memoryResponse.getPhotos())
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
