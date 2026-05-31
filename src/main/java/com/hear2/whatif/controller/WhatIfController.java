package com.hear2.whatif.controller;

import com.hear2.whatif.dto.WhatIfHistoryResponse;
import com.hear2.whatif.dto.WhatIfResponse;
import com.hear2.whatif.dto.WhatIfSimulateRequest;
import com.hear2.whatif.service.WhatIfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/whatif")
public class WhatIfController {

    private final WhatIfService whatIfService;

    @PostMapping("/simulate")
    public WhatIfResponse simulate(
            Authentication authentication,
            @Valid @RequestBody WhatIfSimulateRequest request
    ) {
        return whatIfService.simulate(currentUserId(authentication), request);
    }

    @GetMapping("/history")
    public WhatIfHistoryResponse history(Authentication authentication) {
        return whatIfService.getHistory(currentUserId(authentication));
    }

    @GetMapping("/{id}")
    public WhatIfResponse detail(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return whatIfService.getDetail(currentUserId(authentication), id);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
