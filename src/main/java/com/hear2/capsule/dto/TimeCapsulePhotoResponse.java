package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TimeCapsulePhotoResponse {

    @Schema(description = "캡슐 사진 ID", example = "1")
    private Long id;

    @Schema(description = "open 상태에서만 발급되는 사진 signed URL. sealed 상태에서는 응답되지 않습니다.", nullable = true)
    private String url;

    @Schema(description = "R2/S3 objectKey", example = "media/capsule/17/20260513/9d2c-photo.jpg")
    private String objectKey;

    @Schema(description = "사진 캡션. 현재 생성 API에서는 사용하지 않으며 추후 확장용입니다.", nullable = true)
    private String caption;

    @Schema(description = "사진 촬영 시각. 현재 생성 API에서는 사용하지 않으며 추후 확장용입니다.", nullable = true)
    private LocalDateTime capturedAt;

    @Schema(description = "캡슐 안 사진 순서", example = "0")
    private int orderIndex;
}
