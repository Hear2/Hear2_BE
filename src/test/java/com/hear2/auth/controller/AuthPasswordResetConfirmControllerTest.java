package com.hear2.auth.controller;

import com.hear2.auth.entity.PasswordResetToken;
import com.hear2.auth.repository.PasswordResetTokenRepository;
import com.hear2.auth.repository.RefreshTokenRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetConfirmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void passwordResetConfirmChangesPasswordAndDeletesToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("confirm-reset-user@example.com")
                .password(passwordEncoder.encode("old-password"))
                .nickname("confirm-reset-user")
                .provider("LOCAL")
                .emailVerified(true)
                .build());
        String rawToken = "valid-confirm-reset-token";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getUserId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        confirm(rawToken, "new-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User changedUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(passwordEncoder.matches("new-password", changedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("old-password", changedUser.getPassword())).isFalse();
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "confirm-reset-user@example.com",
                                  "password": "new-password"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetConfirmRejectsMissingToken() throws Exception {
        confirm("missing-confirm-reset-token", "new-password")
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetConfirmRejectsExpiredToken() throws Exception {
        User user = userRepository.save(User.builder()
                .email("expired-confirm-reset-user@example.com")
                .password(passwordEncoder.encode("old-password"))
                .nickname("expired-confirm-reset-user")
                .provider("LOCAL")
                .build());
        String rawToken = "expired-confirm-reset-token";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getUserId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        confirm(rawToken, "new-password")
                .andExpect(status().isBadRequest());

        User unchangedUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(passwordEncoder.matches("old-password", unchangedUser.getPassword())).isTrue();
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s",
                          "newPassword": "%s"
                        }
                        """.formatted(token, newPassword)));
    }

    private String hashToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
