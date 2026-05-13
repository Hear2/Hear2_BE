package com.hear2.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Builder
@Schema(description = "미디어 직접 업로드용 presigned URL 발급 응답")
public class MediaPresignedUrlResponse {

    @Schema(description = "프론트가 PUT으로 직접 업로드할 URL")
    private String uploadUrl;

    @Schema(description = "업로드 HTTP method", example = "PUT")
    private String method;

    @Schema(description = "업로드 때 그대로 넣어야 하는 헤더")
    private Map<String, String> headers;

    @Schema(description = "백엔드 메타데이터 저장 요청에 전달할 object key")
    private String objectKey;

    @Schema(description = "업로드 URL 만료 시각")
    private OffsetDateTime expiresAt;
}
