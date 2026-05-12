package com.hear2.auth.controller;

import com.hear2.auth.entity.PasswordResetToken;
import com.hear2.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetVerifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
    }

    @Test
    void passwordResetVerifyReturnsTrueForUnexpiredToken() throws Exception {
        String rawToken = "valid-reset-token";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(1L)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        verify(rawToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void passwordResetVerifyReturnsFalseForExpiredToken() throws Exception {
        String rawToken = "expired-reset-token";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(2L)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        verify(rawToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void passwordResetVerifyReturnsFalseForMissingToken() throws Exception {
        verify("missing-reset-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void passwordResetVerifyReturnsFalseForBlankToken() throws Exception {
        verify(" ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s"
                        }
                        """.formatted(token)));
    }

    private String hashToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
