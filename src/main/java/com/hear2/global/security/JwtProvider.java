package com.hear2.global.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final String secret;
    private final Long accessTokenExpirationSeconds;
    private final Long refreshTokenExpirationSeconds;

    public JwtProvider(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.access-token-expiration-seconds:}") Long accessTokenExpirationSeconds,
            @Value("${app.jwt.refresh-token-expiration-seconds:}") Long refreshTokenExpirationSeconds
    ) {
        this.secret = secret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @PostConstruct
    public void validateProperties() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret is required");
        }
        if (accessTokenExpirationSeconds == null || accessTokenExpirationSeconds <= 0) {
            throw new IllegalStateException("app.jwt.access-token-expiration-seconds must be positive");
        }
        if (refreshTokenExpirationSeconds == null || refreshTokenExpirationSeconds <= 0) {
            throw new IllegalStateException("app.jwt.refresh-token-expiration-seconds must be positive");
        }
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenExpirationSeconds, ACCESS_TOKEN_TYPE);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenExpirationSeconds, REFRESH_TOKEN_TYPE);
    }

    private String createToken(Long userId, Long expirationSeconds, String tokenType) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + userId + "\",\"token_type\":\"" + tokenType
                + "\",\"jti\":\"" + UUID.randomUUID() + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt + "}");
        String unsignedToken = header + "." + payload;

        return unsignedToken + "." + sign(unsignedToken);
    }

    public Long getUserId(String token) {
        validateToken(token, ACCESS_TOKEN_TYPE);

        String payloadJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        String subject = extractStringClaim(payloadJson, "sub");
        return Long.valueOf(subject);
    }

    public Long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public Long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private void validateToken(String token, String expectedTokenType) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw unauthorized();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!sign(unsignedToken).equals(parts[2])) {
            throw unauthorized();
        }

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tokenType = extractOptionalStringClaim(payloadJson, "token_type");
        if (tokenType != null && !expectedTokenType.equals(tokenType)) {
            throw unauthorized();
        }

        long expiresAt = extractLongClaim(payloadJson, "exp");
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw unauthorized();
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign jwt", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String extractStringClaim(String payloadJson, String claimName) {
        String value = extractOptionalStringClaim(payloadJson, claimName);
        if (value == null) {
            throw unauthorized();
        }

        return value;
    }

    private String extractOptionalStringClaim(String payloadJson, String claimName) {
        String marker = "\"" + claimName + "\":\"";
        int start = payloadJson.indexOf(marker);
        if (start < 0) {
            return null;
        }

        int valueStart = start + marker.length();
        int valueEnd = payloadJson.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            throw unauthorized();
        }

        return payloadJson.substring(valueStart, valueEnd);
    }

    private long extractLongClaim(String payloadJson, String claimName) {
        String marker = "\"" + claimName + "\":";
        int start = payloadJson.indexOf(marker);
        if (start < 0) {
            throw unauthorized();
        }

        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < payloadJson.length() && Character.isDigit(payloadJson.charAt(valueEnd))) {
            valueEnd++;
        }
        if (valueStart == valueEnd) {
            throw unauthorized();
        }

        return Long.parseLong(payloadJson.substring(valueStart, valueEnd));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
    }
}
