package com.hear2.notification.controller;

import com.hear2.notification.dto.FcmTokenRequest;
import com.hear2.notification.dto.FcmTokenResponse;
import com.hear2.notification.service.FcmTokenService;
import com.hear2.global.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/fcm-tokens")
@RequiredArgsConstructor
@Tag(name = "FCM Notification", description = "FCM 토큰 등록과 해제 API")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 등록", description = "사용자의 기기 FCM 토큰을 등록하거나 기존 토큰을 재활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FCM 토큰 등록 성공",
                    content = @Content(schema = @Schema(implementation = FcmTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 요청 값 누락",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public FcmTokenResponse register(@RequestBody FcmTokenRequest request) {
        return fcmTokenService.register(request);
    }

    @Operation(summary = "FCM 토큰 해제", description = "전달받은 FCM 토큰을 비활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "FCM 토큰 해제 성공"),
            @ApiResponse(responseCode = "400", description = "토큰 누락",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestBody FcmTokenRequest request) {
        fcmTokenService.unregister(request);
    }
}
