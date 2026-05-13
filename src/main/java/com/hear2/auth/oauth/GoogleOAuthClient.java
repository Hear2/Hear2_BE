package com.hear2.auth.oauth;

public interface GoogleOAuthClient {

    GoogleOAuthUserInfo verifyIdToken(String idToken);
}
