package com.hear2.couple.controller;

import com.hear2.couple.dto.CoupleConnectRequest;
import com.hear2.couple.dto.CoupleNicknameRequest;
import com.hear2.couple.dto.CoupleNicknamesResponse;
import com.hear2.couple.dto.CoupleStartDateRequest;
import com.hear2.couple.dto.CoupleStatusResponse;
import com.hear2.couple.service.CoupleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
@Tag(name = "Couple", description = "커플 연결, 상태, 사귄 날, 애칭 API")
public class CoupleController {

    private final CoupleService coupleService;

    @PostMapping("/code")
    public CoupleStatusResponse createCode(Authentication authentication) {
        return coupleService.createCode(currentUserId(authentication));
    }

    @PostMapping("/connect")
    public CoupleStatusResponse connect(
            Authentication authentication,
            @Valid @RequestBody CoupleConnectRequest request
    ) {
        return coupleService.connect(currentUserId(authentication), request);
    }

    @GetMapping("/status")
    public CoupleStatusResponse status(Authentication authentication) {
        return coupleService.getStatus(currentUserId(authentication));
    }

    @Operation(
            summary = "커플 사귄 날 설정",
            description = "사귄 날짜를 저장합니다. 저장된 startDate는 커플 상태 조회와 기념일 자동 계산에 사용됩니다."
    )
    @PatchMapping("/start-date")
    public CoupleStatusResponse updateStartDate(
            Authentication authentication,
            @Valid @RequestBody CoupleStartDateRequest request
    ) {
        return coupleService.updateStartDate(currentUserId(authentication), request);
    }

    @GetMapping("/nicknames")
    public CoupleNicknamesResponse getNicknames(Authentication authentication) {
        return coupleService.getNicknames(currentUserId(authentication));
    }

    @PatchMapping("/nicknames")
    public CoupleNicknamesResponse updateNickname(
            Authentication authentication,
            @Valid @RequestBody CoupleNicknameRequest request
    ) {
        return coupleService.updateNickname(currentUserId(authentication), request);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
