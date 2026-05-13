package com.hear2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoOAuthLoginRequest {

    @NotBlank
    private String accessToken;
}
