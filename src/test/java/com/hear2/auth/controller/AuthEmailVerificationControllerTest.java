package com.hear2.auth.controller;

import com.hear2.auth.entity.EmailVerificationToken;
import com.hear2.auth.repository.EmailVerificationTokenRepository;
import com.hear2.auth.repository.RefreshTokenRepository;
import com.hear2.global.mail.EmailService;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthEmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        reset(emailService);
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesUnverifiedUserAndSendsVerificationToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup-verify-user@example.com",
                                  "password": "password",
                                  "nickname": "signup-verify-user",
                                  "provider": "LOCAL"
                                }
                                """))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("signup-verify-user@example.com").orElseThrow();
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(emailVerificationTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getUserId()).isEqualTo(user.getUserId());
                    assertThat(token.getTokenHash()).hasSize(44);
                    assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
                });

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailVerificationEmail(eq("signup-verify-user@example.com"), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isNotBlank();
        assertThat(emailVerificationTokenRepository.findAll().get(0).getTokenHash()).isEqualTo(hashToken(tokenCaptor.getValue()));
    }

    @Test
    void emailVerifyMarksUserVerifiedAndDeletesToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("email-verify-user@example.com")
                .password("encoded-password")
                .nickname("email-verify-user")
                .provider("LOCAL")
                .build());
        String rawToken = "valid-email-verification-token";
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getUserId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        verifyEmail(rawToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User verifiedUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(verifiedUser.getEmailVerified()).isTrue();
        assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
    }

    @Test
    void emailVerifyRejectsExpiredToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("expired-email-verify-user@example.com")
                .password("encoded-password")
                .nickname("expired-email-verify-user")
                .provider("LOCAL")
                .build());
        String rawToken = "expired-email-verification-token";
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getUserId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        verifyEmail(rawToken)
                .andExpect(status().isBadRequest());

        User unchangedUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(unchangedUser.getEmailVerified()).isFalse();
    }

    @Test
    void emailVerifyRejectsMissingToken() throws Exception {
        verifyEmail("missing-email-verification-token")
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailVerificationResendRotatesTokenForUnverifiedUser() throws Exception {
        User user = userRepository.save(User.builder()
                .email("resend-email-verify-user@example.com")
                .password("encoded-password")
                .nickname("resend-email-verify-user")
                .provider("LOCAL")
                .build());
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getUserId())
                .tokenHash(hashToken("old-email-verification-token"))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());
        EmailVerificationToken oldToken = emailVerificationTokenRepository.findByUserId(user.getUserId()).orElseThrow();

        resendEmailVerification("resend-email-verify-user@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(emailVerificationTokenRepository.findAll()).hasSize(1);
        EmailVerificationToken rotatedToken = emailVerificationTokenRepository.findByUserId(user.getUserId()).orElseThrow();
        assertThat(rotatedToken.getEmailVerificationTokenId()).isEqualTo(oldToken.getEmailVerificationTokenId());
        assertThat(rotatedToken.getTokenHash()).hasSize(44);
        assertThat(rotatedToken.getTokenHash()).isNotEqualTo(oldToken.getTokenHash());
        assertThat(rotatedToken.getExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailVerificationEmail(eq("resend-email-verify-user@example.com"), tokenCaptor.capture());
        assertThat(rotatedToken.getTokenHash()).isEqualTo(hashToken(tokenCaptor.getValue()));
    }

    @Test
    void emailVerificationResendRejectsAlreadyVerifiedUser() throws Exception {
        userRepository.save(User.builder()
                .email("already-verified-user@example.com")
                .password("encoded-password")
                .nickname("already-verified-user")
                .provider("LOCAL")
                .emailVerified(true)
                .build());

        resendEmailVerification("already-verified-user@example.com")
                .andExpect(status().isConflict());

        assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString());
    }

    @Test
    void emailVerificationResendRejectsMissingUser() throws Exception {
        resendEmailVerification("missing-email-verify-user@example.com")
                .andExpect(status().isNotFound());

        assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString());
    }

    private org.springframework.test.web.servlet.ResultActions verifyEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s"
                        }
                        """.formatted(token)));
    }

    private org.springframework.test.web.servlet.ResultActions resendEmailVerification(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email)));
    }

    private String hashToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
