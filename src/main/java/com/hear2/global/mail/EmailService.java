package com.hear2.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private static final String RESEND_EMAILS_PATH = "/emails";
    private static final String RESEND_PROVIDER = "resend";
    private static final String PASSWORD_RESET_SUBJECT = "Hear2 \uBE44\uBC00\uBC88\uD638 \uC7AC\uC124\uC815 \uC778\uC99D \uD1A0\uD070";
    private static final String PASSWORD_RESET_HTML_FORMAT = "<p>\uBE44\uBC00\uBC88\uD638 \uC7AC\uC124\uC815 \uC778\uC99D \uD1A0\uD070: %s</p>";
    private static final String EMAIL_VERIFICATION_SUBJECT = "Hear2 \uC774\uBA54\uC77C \uC778\uC99D \uCF54\uB4DC";
    private static final String EMAIL_VERIFICATION_HTML_FORMAT = "<p>\uC778\uC99D \uCF54\uB4DC: %s</p>";

    private final RestClient restClient;
    private final String provider;
    private final String apiKey;
    private final String from;

    @Autowired
    public EmailService(
            @Value("${app.mail.provider:resend}") String provider,
            @Value("${app.mail.resend.api-key:}") String apiKey,
            @Value("${app.mail.resend.from:onboarding@resend.dev}") String from,
            @Value("${app.mail.resend.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.mail.resend.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        this(buildRestClient(connectTimeoutMs, readTimeoutMs), provider, apiKey, from);
    }

    EmailService(RestClient restClient, String provider, String apiKey, String from) {
        this.restClient = restClient;
        this.provider = provider;
        this.apiKey = apiKey;
        this.from = from;
    }

    private static RestClient buildRestClient(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl("https://api.resend.com")
                .requestFactory(requestFactory)
                .build();
    }

    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            sendResendEmail(to, PASSWORD_RESET_SUBJECT, PASSWORD_RESET_HTML_FORMAT.formatted(escapeHtml(resetToken)));
        } catch (RuntimeException exception) {
            throw new EmailSendException("failed to send password reset email", exception);
        }
    }

    public void sendEmailVerificationEmail(String to, String token) {
        try {
            sendResendEmail(to, EMAIL_VERIFICATION_SUBJECT, EMAIL_VERIFICATION_HTML_FORMAT.formatted(escapeHtml(token)));
        } catch (RuntimeException exception) {
            log.warn("Failed to send verification email through Resend. email={}, reason={}", to, exception.getMessage());
        }
    }

    private void sendResendEmail(String to, String subject, String html) {
        validateResendConfiguration();

        restClient.post()
                .uri(RESEND_EMAILS_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", from,
                        "to", List.of(to),
                        "subject", subject,
                        "html", html
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private void validateResendConfiguration() {
        if (!RESEND_PROVIDER.equalsIgnoreCase(provider)) {
            throw new IllegalStateException("unsupported mail provider: " + provider);
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("resend api key is empty");
        }
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("resend from email is empty");
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
