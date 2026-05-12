package com.hear2.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetVerifyResponse {

    private boolean valid;

    public static PasswordResetVerifyResponse of(boolean valid) {
        return PasswordResetVerifyResponse.builder()
                .valid(valid)
                .build();
    }
}
