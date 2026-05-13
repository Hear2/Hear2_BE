package com.hear2.auth.oauth;

public record GoogleOAuthUserInfo(
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
}
