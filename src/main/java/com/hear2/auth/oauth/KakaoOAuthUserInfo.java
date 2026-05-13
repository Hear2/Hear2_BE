package com.hear2.auth.oauth;

public record KakaoOAuthUserInfo(
        String providerId,
        String email,
        boolean emailVerified,
        String nickname,
        String profileImage
) {
}
