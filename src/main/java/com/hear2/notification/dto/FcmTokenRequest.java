package com.hear2.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "FCM 토큰 등록/해제 요청")
public class FcmTokenRequest {

    @Schema(description = "사용자 ID. 등록 시 필수입니다.", example = "11")
    private Long userId;

    @Schema(description = "FCM 디바이스 토큰", example = "fcm-token-sample")
    private String token;

    @Schema(description = "기기 식별자", example = "iphone-15-pro")
    private String deviceId;

    @Schema(description = "플랫폼", example = "ios")
    private String platform;
}
