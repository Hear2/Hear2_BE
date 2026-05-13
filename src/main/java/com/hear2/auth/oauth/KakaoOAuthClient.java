package com.hear2.auth.oauth;

public interface KakaoOAuthClient {

    KakaoOAuthUserInfo getUserInfo(String accessToken);
}
