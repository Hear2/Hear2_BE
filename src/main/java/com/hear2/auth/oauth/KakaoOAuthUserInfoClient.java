package com.hear2.auth.oauth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class KakaoOAuthUserInfoClient implements KakaoOAuthClient {

    private static final String USER_INFO_PATH = "/v2/user/me";

    private final RestClient restClient;

    public KakaoOAuthUserInfoClient() {
        this(RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build());
    }

    KakaoOAuthUserInfoClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public KakaoOAuthUserInfo getUserInfo(String accessToken) {
        Map<?, ?> response = requestUserInfo(accessToken);
        Map<?, ?> kakaoAccount = asMap(response.get("kakao_account"));
        Map<?, ?> profile = asMap(kakaoAccount.get("profile"));

        String providerId = asString(response.get("id"));
        if (!StringUtils.hasText(providerId)) {
            throw invalidKakaoUserInfoResponse();
        }

        String email = asString(kakaoAccount.get("email"));

        return new KakaoOAuthUserInfo(
                providerId,
                email,
                isEmailVerified(kakaoAccount),
                asString(profile.get("nickname")),
                asString(profile.get("profile_image_url"))
        );
    }

    private Map<?, ?> requestUserInfo(String accessToken) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(USER_INFO_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw invalidKakaoAccessToken();
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw invalidKakaoAccessToken();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to request kakao user info", exception);
        }
    }

    private boolean isEmailVerified(Map<?, ?> kakaoAccount) {
        Object emailVerified = kakaoAccount.get("email_verified");
        if (emailVerified == null) {
            emailVerified = kakaoAccount.get("is_email_verified");
        }
        return emailVerified == null || asBoolean(emailVerified);
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> mapValue ? mapValue : Map.of();
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return false;
    }

    private String asString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.toString();
        }
        return "";
    }

    private ResponseStatusException invalidKakaoAccessToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid kakao access token");
    }

    private ResponseStatusException invalidKakaoUserInfoResponse() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid kakao user info response");
    }
}
