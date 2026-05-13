package com.hear2.location.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.location.dto.LocationSaveResponse;
import com.hear2.location.dto.LocationShareStatusRequest;
import com.hear2.location.dto.LocationShareStatusResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.dto.PartnerLocationResponse;
import com.hear2.location.service.LocationShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Location Sharing", description = "커플 간 선택적 실시간 위치 공유 API")
public class LocationShareController {

    private final LocationShareService locationShareService;

    @Operation(summary = "내 위치 업로드", description = "JWT의 로그인 사용자 기준으로 현재 위치를 저장합니다. userId와 coupleId는 요청하지 않습니다.")
    @PostMapping("/api/v1/location")
    public ApiResponse<LocationSaveResponse> updateCurrentLocation(
            Authentication authentication,
            @Valid @RequestBody LocationUpdateRequest request
    ) {
        return ApiResponse.success(LocationSaveResponse.saved(
                locationShareService.updateCurrentLocation(currentUserId(authentication), request).getUpdatedAt()
        ));
    }

    @Operation(summary = "커플 양쪽 위치 조회", description = "로그인 사용자의 커플 기준으로 내 위치와 상대 위치를 조회합니다. 내가 위치 공유를 끄면 403으로 차단되고, 상대가 끄면 partner는 null입니다.")
    @GetMapping("/api/v1/couple/location")
    public ApiResponse<PartnerLocationResponse> getCoupleLocation(Authentication authentication) {
        return ApiResponse.success(locationShareService.getCoupleLocation(currentUserId(authentication)));
    }

    @Operation(summary = "위치 공유 ON/OFF 변경", description = "로그인 사용자의 위치 공유 상태를 변경합니다. OFF로 변경하면 저장된 마지막 위치를 즉시 삭제합니다.")
    @PutMapping("/api/v1/location/sharing")
    public ApiResponse<LocationShareStatusResponse> updateShareStatus(
            Authentication authentication,
            @Valid @RequestBody LocationShareStatusRequest request
    ) {
        return ApiResponse.success(locationShareService.updateShareStatus(currentUserId(authentication), request));
    }

    @Operation(summary = "내 위치 공유 상태 조회", description = "로그인 사용자의 위치 공유 ON/OFF 상태를 조회합니다.")
    @GetMapping("/api/v1/location/sharing")
    public ApiResponse<LocationShareStatusResponse> getShareStatus(Authentication authentication) {
        return ApiResponse.success(locationShareService.getShareStatus(currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }

        return userId;
    }
}
