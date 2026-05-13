package com.hear2.media.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.media.dto.MediaPresignedUrlRequest;
import com.hear2.media.dto.MediaPresignedUrlResponse;
import com.hear2.media.service.MediaPresignedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Media", description = "사진/음성 직접 업로드용 presigned URL API")
public class MediaController {

    private final MediaPresignedUrlService mediaPresignedUrlService;

    @Operation(
            summary = "미디어 업로드 URL 발급",
            description = "프론트가 R2/S3-compatible 스토리지에 직접 PUT 업로드할 presigned URL을 발급합니다. 응답의 objectKey를 이후 추억/채팅 메타데이터 저장 API에 전달합니다."
    )
    @PostMapping({"/media/presigned-url", "/api/v1/media/presigned-url"})
    public ApiResponse<MediaPresignedUrlResponse> createPresignedUrl(
            Authentication authentication,
            @Valid @RequestBody MediaPresignedUrlRequest request
    ) {
        return ApiResponse.success(mediaPresignedUrlService.createUploadUrl(request, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
