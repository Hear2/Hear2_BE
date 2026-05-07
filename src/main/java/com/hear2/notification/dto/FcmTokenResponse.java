package com.hear2.notification.dto;

import com.hear2.notification.entity.FcmToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "FCM 토큰 등록 응답")
public class FcmTokenResponse {

    @Schema(description = "FCM 토큰 레코드 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "11")
    private Long userId;

    @Schema(description = "기기 식별자", example = "iphone-15-pro")
    private String deviceId;

    @Schema(description = "플랫폼", example = "ios")
    private String platform;

    @Schema(description = "토큰 활성화 여부", example = "true")
    private Boolean active;

    public static FcmTokenResponse from(FcmToken fcmToken) {
        return FcmTokenResponse.builder()
                .id(fcmToken.getId())
                .userId(fcmToken.getUserId())
                .deviceId(fcmToken.getDeviceId())
                .platform(fcmToken.getPlatform())
                .active(fcmToken.getActive())
                .build();
    }
}
