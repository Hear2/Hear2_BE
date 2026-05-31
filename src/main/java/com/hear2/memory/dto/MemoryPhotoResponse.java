package com.hear2.memory.dto;

import com.hear2.memory.entity.MemoryPhoto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추억 사진 항목")
public class MemoryPhotoResponse {

    @Schema(description = "사진 ID. 기존 단일 사진 호환 항목이면 null일 수 있습니다.", example = "12")
    private Long id;

    @Schema(description = "사진 표시 URL. R2 사용 시 만료 시간이 있는 presigned GET URL입니다.")
    private String url;

    @Schema(description = "R2/S3 object key 또는 외부 이미지 URL")
    private String objectKey;

    @Schema(description = "원본 파일명", example = "memory.jpg")
    private String originalFileName;

    @Schema(description = "MIME 타입", example = "image/jpeg")
    private String contentType;

    @Schema(description = "파일 크기(byte)", example = "204800")
    private Long size;

    @Schema(description = "캐러셀 표시 순서. 0부터 시작합니다.", example = "0")
    private int order;

    public static MemoryPhotoResponse from(MemoryPhoto photo, String url) {
        return MemoryPhotoResponse.builder()
                .id(photo.getId())
                .url(url)
                .objectKey(photo.getStoredPhotoPath())
                .originalFileName(photo.getOriginalFileName())
                .contentType(photo.getPhotoContentType())
                .size(photo.getPhotoSize())
                .order(photo.getSortOrder())
                .build();
    }

    public static MemoryPhotoResponse cover(
            String url,
            String objectKey,
            String originalFileName,
            String contentType,
            Long size
    ) {
        return MemoryPhotoResponse.builder()
                .url(url)
                .objectKey(objectKey)
                .originalFileName(originalFileName)
                .contentType(contentType)
                .size(size)
                .order(0)
                .build();
    }
}
