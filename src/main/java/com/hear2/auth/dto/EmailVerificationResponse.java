package com.hear2.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationResponse {

    private boolean success;

    public static EmailVerificationResponse success() {
        return EmailVerificationResponse.builder()
                .success(true)
                .build();
    }
}
