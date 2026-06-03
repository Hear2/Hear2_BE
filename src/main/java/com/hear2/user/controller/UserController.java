package com.hear2.user.controller;

import com.hear2.auth.dto.MeResponse;
import com.hear2.user.dto.UserProfileUpdateRequest;
import com.hear2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 프로필 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 수정", description = "요청에 포함된 필드만 수정하고 전체 프로필을 반환합니다.")
    @PatchMapping("/me")
    public MeResponse updateMe(
            Authentication authentication,
            @RequestBody(required = false) UserProfileUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return userService.updateMe(userId, request);
    }
}
