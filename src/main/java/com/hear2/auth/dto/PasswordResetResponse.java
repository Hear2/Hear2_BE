package com.hear2.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetResponse {

    private boolean success;

    public static PasswordResetResponse success() {
        return PasswordResetResponse.builder()
                .success(true)
                .build();
    }
}
