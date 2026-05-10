package com.hear2.auth.dto;

import com.hear2.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthResponse {

    private Long userId;

    private String email;

    private String nickname;

    private String profileImage;

    private String provider;

    private LocalDateTime createdAt;

    private TokenResponse token;

    public static AuthResponse from(User user, TokenResponse token) {
        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();
    }
}
