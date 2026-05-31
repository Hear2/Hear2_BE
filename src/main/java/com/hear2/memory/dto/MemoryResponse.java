package com.hear2.memory.dto;

import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiAnalysisStatus;
import com.hear2.memory.entity.MemoryTagSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@Schema(description = "추억 앨범 항목 응답")
public class MemoryResponse {

    private Long id;
    private Long coupleId;
    private Long uploaderId;
    private String memo;
    private LocalDate memoryDate;
    private String originalFileName;
    private String photoContentType;
    private Long photoSize;
    @Schema(description = "대표 사진 URL. R2 사용 시 인증 헤더 없이 접근 가능한 presigned GET URL입니다.")
    private String photoUrl;
    private boolean photoAvailable;
    @Schema(description = "한 게시물에 포함된 사진 배열. 상세 화면 캐러셀은 이 배열을 사용합니다.")
    private List<MemoryPhotoResponse> photos;
    private MemoryAiAnalysisStatus aiAnalysisStatus;
    private MemoryPhotoMetadataResponse metadata;
    private List<MemoryAiTagResponse> tags;
    private List<String> aiTags;
    private List<String> userTags;
    private List<MemoryCommentResponse> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemoryResponse from(Memory memory) {
        return from(memory, storedPhotoPath -> resolvePhotoUrl(storedPhotoPath, memory.getId()));
    }

    public static MemoryResponse from(Memory memory, Function<String, String> photoUrlResolver) {
        String coverPhotoUrl = photoUrlResolver.apply(memory.getStoredPhotoPath());

        return MemoryResponse.builder()
                .id(memory.getId())
                .coupleId(memory.getCoupleId())
                .uploaderId(memory.getUploaderId())
                .memo(memory.getMemo())
                .memoryDate(memory.getMemoryDate())
                .originalFileName(memory.getOriginalFileName())
                .photoContentType(memory.getPhotoContentType())
                .photoSize(memory.getPhotoSize())
                .photoUrl(coverPhotoUrl)
                .photoAvailable(memory.getStoredPhotoPath() != null)
                .photos(resolvePhotos(memory, photoUrlResolver, coverPhotoUrl))
                .aiAnalysisStatus(memory.getAiAnalysisStatus())
                .metadata(MemoryPhotoMetadataResponse.from(memory.getPhotoMetadata()))
                .tags(memory.getAiTags().stream()
                        .map(MemoryAiTagResponse::from)
                        .toList())
                .aiTags(memory.getAiTags().stream()
                        .filter(tag -> tag.getSource() == MemoryTagSource.AI)
                        .map(tag -> "#" + tag.getTagName())
                        .toList())
                .userTags(memory.getAiTags().stream()
                        .filter(tag -> tag.getSource() == MemoryTagSource.USER)
                        .map(tag -> "#" + tag.getTagName())
                        .toList())
                .comments(List.of())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private static List<MemoryPhotoResponse> resolvePhotos(
            Memory memory,
            Function<String, String> photoUrlResolver,
            String coverPhotoUrl
    ) {
        if (memory.getPhotos() == null || memory.getPhotos().isEmpty()) {
            if (memory.getStoredPhotoPath() == null) {
                return List.of();
            }

            return List.of(MemoryPhotoResponse.cover(
                    coverPhotoUrl,
                    memory.getStoredPhotoPath(),
                    memory.getOriginalFileName(),
                    memory.getPhotoContentType(),
                    memory.getPhotoSize()
            ));
        }

        return memory.getPhotos().stream()
                .map(photo -> MemoryPhotoResponse.from(
                        photo,
                        photoUrlResolver.apply(photo.getStoredPhotoPath())
                ))
                .toList();
    }

    public static MemoryResponse from(Memory memory, List<MemoryCommentResponse> comments) {
        return from(memory, comments, storedPhotoPath -> resolvePhotoUrl(storedPhotoPath, memory.getId()));
    }

    public static MemoryResponse from(
            Memory memory,
            List<MemoryCommentResponse> comments,
            Function<String, String> photoUrlResolver
    ) {
        MemoryResponse response = from(memory, photoUrlResolver);
        response.comments = comments == null ? List.of() : comments;
        return response;
    }

    public static String resolvePhotoUrl(Memory memory) {
        return resolvePhotoUrl(memory.getStoredPhotoPath(), memory.getId());
    }

    public static String resolvePhotoUrl(String storedPhotoPath, Long memoryId) {
        if (memoryId == null || storedPhotoPath == null) {
            return null;
        }

        if (isExternalReference(storedPhotoPath)) {
            return storedPhotoPath;
        }

        return "/api/v1/memories/items/"
                + memoryId
                + "/photo";
    }

    public static String resolvePhotoUrl(String storedPhotoPath) {
        if (storedPhotoPath == null) {
            return null;
        }

        if (isExternalReference(storedPhotoPath)) {
            return storedPhotoPath;
        }

        return null;
    }

    private static boolean isExternalReference(String storedPhotoPath) {
        return storedPhotoPath.startsWith("http://")
                || storedPhotoPath.startsWith("https://")
                || storedPhotoPath.startsWith("s3://");
    }
}
