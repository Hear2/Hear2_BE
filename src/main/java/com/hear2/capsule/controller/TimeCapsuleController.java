package com.hear2.capsule.controller;

import com.hear2.capsule.dto.TimeCapsuleCreateRequest;
import com.hear2.capsule.dto.TimeCapsuleDetailResponse;
import com.hear2.capsule.dto.TimeCapsuleListResponse;
import com.hear2.capsule.dto.TimeCapsuleShareCardResponse;
import com.hear2.capsule.service.TimeCapsuleService;
import com.hear2.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/capsule")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Time Capsule",
        description = """
                커플 타임캡슐 생성, 조회, 자동 오픈 API입니다.
                모든 API는 JWT 로그인이 필요하며, Swagger Authorize에는 Bearer 없이 accessToken만 입력하세요.
                coupleId와 userId는 토큰 기준으로 자동 적용되므로 요청에 직접 넣지 않습니다.
                """
)
public class TimeCapsuleController {

    private final TimeCapsuleService timeCapsuleService;

    @Operation(
            summary = "타임캡슐 생성",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = """
                    로그인된 사용자의 커플 기준으로 타임캡슐을 생성합니다.

                    테스트 순서:
                    1. /api/v1/auth/login 또는 OAuth 로그인으로 accessToken을 받습니다.
                    2. Swagger 우측 Authorize에 Bearer 없이 accessToken만 입력합니다.
                    3. 사진이 있으면 먼저 POST /api/v1/media/presigned-url 로 uploadUrl/objectKey를 발급받습니다.
                    4. 프론트 또는 별도 HTTP 클라이언트가 uploadUrl에 직접 PUT 업로드합니다.
                    5. 이 API의 photoObjectKeys에는 업로드 후 받은 objectKey만 넣습니다.

                    주의:
                    - coupleId/userId는 JWT에서 자동 적용됩니다.
                    - openAt은 UTC ISO8601 형식 예: 2027-05-16T00:00:00Z
                    - objectKey가 아직 실제 R2에 없어도 캡슐 메타데이터 저장은 가능하지만, open 이후 사진 signed URL은 실제 파일이 있어야 정상 조회됩니다.
                    """
    )
    @PostMapping
    public ApiResponse<TimeCapsuleDetailResponse> createCapsule(
            Authentication authentication,
            @Valid @RequestBody TimeCapsuleCreateRequest request
    ) {
        return ApiResponse.success(timeCapsuleService.createCapsule(request, currentUserId(authentication)));
    }

    @Operation(
            summary = "내 커플 타임캡슐 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = """
                    로그인된 사용자의 커플 기준으로 목록을 조회합니다.
                    status는 sealed, open, all 중 하나이며 생략하면 all입니다.
                    openAt이 지난 sealed 캡슐은 조회 시 자동으로 open 처리됩니다.
                    """
    )
    @GetMapping
    public ApiResponse<TimeCapsuleListResponse> getCapsules(
            Authentication authentication,
            @Parameter(
                    description = "조회할 캡슐 상태. sealed, open, all 중 하나",
                    example = "all",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "all") String status
    ) {
        return ApiResponse.success(timeCapsuleService.getCapsules(status, currentUserId(authentication)));
    }

    @Operation(
            summary = "타임캡슐 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = """
                    로그인된 사용자의 커플 캡슐만 조회할 수 있습니다.
                    sealed 상태에서는 편지와 사진 URL, then-vs-now 데이터가 null/빈 배열로 내려갑니다.
                    open 상태에서는 편지와 사진 signed URL, then-vs-now 비교 데이터가 함께 내려갑니다.
                    """
    )
    @GetMapping("/{capsuleId}")
    public ApiResponse<TimeCapsuleDetailResponse> getCapsule(
            Authentication authentication,
            @Parameter(description = "타임캡슐 ID. 목록 조회 응답의 id 값을 사용합니다.", example = "1")
            @PathVariable Long capsuleId
    ) {
        return ApiResponse.success(timeCapsuleService.getCapsule(capsuleId, currentUserId(authentication)));
    }

    @Operation(
            summary = "인스타 스토리 공유용 타임캡슐 카드 조회",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = """
                    프론트가 인스타 스토리 공유 화면을 만들 때 사용할 카드 메타데이터를 조회합니다.
                    백엔드는 인스타그램 앱을 직접 열 수 없으므로, 프론트가 이 응답으로 9:16 공유 이미지를 만든 뒤 Instagram Stories 공유 기능을 호출해야 합니다.
                    sealed 상태의 캡슐은 공유할 수 없습니다.
                    """
    )
    @GetMapping("/{capsuleId}/share-card")
    public ApiResponse<TimeCapsuleShareCardResponse> getShareCard(
            Authentication authentication,
            @Parameter(description = "타임캡슐 ID. open 상태의 캡슐만 공유 카드 조회가 가능합니다.", example = "1")
            @PathVariable Long capsuleId
    ) {
        return ApiResponse.success(timeCapsuleService.getShareCard(capsuleId, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
