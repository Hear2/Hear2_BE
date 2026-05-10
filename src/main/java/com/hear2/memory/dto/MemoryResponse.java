package com.hear2.memory.dto;

import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiAnalysisStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MemoryResponse {

    private Long id;
    private Long coupleId;
    private Long uploaderId;
    private String memo;
    private LocalDate memoryDate;
    private String originalFileName;
    private String photoContentType;
    private Long photoSize;
    private String photoUrl;
    private boolean photoAvailable;
    private MemoryAiAnalysisStatus aiAnalysisStatus;
    private MemoryPhotoMetadataResponse metadata;
    private List<MemoryAiTagResponse> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemoryResponse from(Memory memory) {
        return MemoryResponse.builder()
                .id(memory.getId())
                .coupleId(memory.getCoupleId())
                .uploaderId(memory.getUploaderId())
                .memo(memory.getMemo())
                .memoryDate(memory.getMemoryDate())
                .originalFileName(memory.getOriginalFileName())
                .photoContentType(memory.getPhotoContentType())
                .photoSize(memory.getPhotoSize())
                .photoUrl(resolvePhotoUrl(memory))
                .photoAvailable(memory.getStoredPhotoPath() != null)
                .aiAnalysisStatus(memory.getAiAnalysisStatus())
                .metadata(MemoryPhotoMetadataResponse.from(memory.getPhotoMetadata()))
                .tags(memory.getAiTags().stream()
                        .map(MemoryAiTagResponse::from)
                        .toList())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private static String resolvePhotoUrl(Memory memory) {
        if (memory.getId() == null || memory.getStoredPhotoPath() == null) {
            return null;
        }

        return "/api/v1/memories/couples/"
                + memory.getCoupleId()
                + "/items/"
                + memory.getId()
                + "/photo";
    }
}
