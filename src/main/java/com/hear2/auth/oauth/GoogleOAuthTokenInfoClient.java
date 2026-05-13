package com.hear2.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class GoogleOAuthTokenInfoClient implements GoogleOAuthClient {

    private static final String TOKEN_INFO_PATH = "/tokeninfo";

    private final RestClient restClient;
    private final String clientId;

    public GoogleOAuthTokenInfoClient(@Value("${google.oauth.client-id:}") String clientId) {
        this.restClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
        this.clientId = clientId;
    }

    @Override
    public GoogleOAuthUserInfo verifyIdToken(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "google oauth client id is not configured");
        }

        Map<?, ?> response = requestTokenInfo(idToken);
        if (response == null) {
            throw invalidGoogleIdToken();
        }

        if (!clientId.equals(asString(response.get("aud")))) {
            throw invalidGoogleIdToken();
        }

        String email = asString(response.get("email"));
        if (!StringUtils.hasText(email)) {
            throw invalidGoogleIdToken();
        }

        return new GoogleOAuthUserInfo(
                email,
                asBoolean(response.get("email_verified")),
                asString(response.get("name")),
                asString(response.get("picture"))
        );
    }

    private Map<?, ?> requestTokenInfo(String idToken) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TOKEN_INFO_PATH)
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException exception) {
            throw invalidGoogleIdToken();
        }
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
        return value instanceof String stringValue ? stringValue : "";
    }

    private ResponseStatusException invalidGoogleIdToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid google id token");
    }
}
