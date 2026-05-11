package com.hear2.couple.controller;

import com.hear2.couple.dto.CoupleConnectRequest;
import com.hear2.couple.dto.CoupleStatusResponse;
import com.hear2.couple.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
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

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
