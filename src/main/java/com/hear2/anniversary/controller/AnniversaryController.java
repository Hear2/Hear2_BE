package com.hear2.anniversary.controller;

import com.hear2.anniversary.dto.AnniversaryCreateRequest;
import com.hear2.anniversary.dto.AnniversaryListResponse;
import com.hear2.anniversary.dto.AnniversaryResponse;
import com.hear2.anniversary.dto.AnniversaryStartDateRequest;
import com.hear2.anniversary.dto.AnniversaryUpdateRequest;
import com.hear2.anniversary.service.AnniversaryService;
import com.hear2.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Anniversary", description = "커플 D-DAY, 자동 기념일, 사용자 지정 기념일 API")
public class AnniversaryController {

    private final AnniversaryService anniversaryService;

    @Operation(
            summary = "커플 시작일 설정",
            description = "사귄 날짜를 저장합니다. 이 날짜를 기준으로 D+와 100일/1주년/n주년 자동 기념일을 계산합니다."
    )
    @PutMapping("/api/v1/anniversaries/start-date")
    public ApiResponse<AnniversaryListResponse> updateStartDate(
            Authentication authentication,
            @Valid @RequestBody AnniversaryStartDateRequest request
    ) {
        return ApiResponse.success(anniversaryService.updateStartDate(request, currentUserId(authentication)));
    }

    @Operation(
            summary = "기념일 목록 조회",
            description = """
                    커플 시작일 기반 자동 기념일과 사용자가 만든 생일/기타 기념일을 함께 조회합니다.
                    자동 기념일은 DB에 저장하지 않고 조회 시 계산하며, autoKey로 숨김 처리할 수 있습니다.
                    """
    )
    @GetMapping("/api/v1/anniversaries")
    public ApiResponse<AnniversaryListResponse> getAnniversaries(Authentication authentication) {
        return ApiResponse.success(anniversaryService.getAnniversaries(currentUserId(authentication)));
    }

    @Operation(summary = "다가오는 기념일 조회", description = "홈 화면 등에 표시할 다가오는 기념일을 가까운 순서로 조회합니다.")
    @GetMapping("/api/v1/anniversaries/upcoming")
    public ApiResponse<List<AnniversaryResponse>> getUpcoming(
            Authentication authentication,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(anniversaryService.getUpcoming(limit, currentUserId(authentication)));
    }

    @Operation(summary = "사용자 지정 기념일 생성", description = "생일, 기타 기념일 등을 직접 저장합니다.")
    @PostMapping("/api/v1/anniversaries")
    public ApiResponse<AnniversaryResponse> createAnniversary(
            Authentication authentication,
            @Valid @RequestBody AnniversaryCreateRequest request
    ) {
        return ApiResponse.success(anniversaryService.createAnniversary(request, currentUserId(authentication)));
    }

    @Operation(summary = "사용자 지정 기념일 수정", description = "본인이 만든 기념일만 수정할 수 있습니다. 자동 기념일은 수정할 수 없습니다.")
    @PatchMapping("/api/v1/anniversaries/{anniversaryId}")
    public ApiResponse<AnniversaryResponse> updateAnniversary(
            Authentication authentication,
            @PathVariable Long anniversaryId,
            @Valid @RequestBody AnniversaryUpdateRequest request
    ) {
        return ApiResponse.success(anniversaryService.updateAnniversary(anniversaryId, request, currentUserId(authentication)));
    }

    @Operation(summary = "사용자 지정 기념일 삭제", description = "본인이 만든 기념일만 삭제할 수 있습니다.")
    @DeleteMapping("/api/v1/anniversaries/{anniversaryId}")
    public ApiResponse<Void> deleteAnniversary(
            Authentication authentication,
            @PathVariable Long anniversaryId
    ) {
        anniversaryService.deleteAnniversary(anniversaryId, currentUserId(authentication));
        return ApiResponse.success(null, "anniversary deleted");
    }

    @Operation(summary = "자동 기념일 숨김", description = "자동 생성 기념일을 현재 사용자 화면에서 숨깁니다. 예: DAY_100, YEAR_3")
    @PostMapping("/api/v1/anniversaries/auto/{autoKey}/hide")
    public ApiResponse<AnniversaryListResponse> hideAutoAnniversary(
            Authentication authentication,
            @PathVariable String autoKey
    ) {
        return ApiResponse.success(anniversaryService.hideAutoAnniversary(autoKey, currentUserId(authentication)));
    }

    @Operation(summary = "자동 기념일 숨김 해제", description = "숨겼던 자동 생성 기념일을 다시 표시합니다.")
    @DeleteMapping("/api/v1/anniversaries/auto/{autoKey}/hide")
    public ApiResponse<AnniversaryListResponse> showAutoAnniversary(
            Authentication authentication,
            @PathVariable String autoKey
    ) {
        return ApiResponse.success(anniversaryService.showAutoAnniversary(autoKey, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
