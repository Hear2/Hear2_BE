package com.hear2.auth.controller;

import com.hear2.auth.entity.PasswordResetToken;
import com.hear2.auth.repository.PasswordResetTokenRepository;
import com.hear2.global.mail.EmailSendException;
import com.hear2.global.mail.EmailService;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        reset(emailService);
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void passwordResetRequestCreatesHashedTokenForExistingUser() throws Exception {
        User user = userRepository.save(User.builder()
                .email("reset-user@example.com")
                .password("encoded-password")
                .nickname("reset-user")
                .provider("LOCAL")
                .build());

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(passwordResetTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getUserId()).isEqualTo(user.getUserId());
                    assertThat(token.getTokenHash()).hasSize(44);
                    assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
                });
        verify(emailService).sendPasswordResetEmail(eq("reset-user@example.com"), anyString());
    }

    @Test
    void passwordResetRequestKeepsOnlyOneTokenPerUser() throws Exception {
        User user = userRepository.save(User.builder()
                .email("rotate-reset-user@example.com")
                .password("encoded-password")
                .nickname("rotate-reset-user")
                .provider("LOCAL")
                .build());

        requestPasswordReset("rotate-reset-user@example.com");
        PasswordResetToken firstToken = passwordResetTokenRepository.findByUserId(user.getUserId()).orElseThrow();

        requestPasswordReset("rotate-reset-user@example.com");

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        assertThat(passwordResetTokenRepository.findByUserId(user.getUserId()))
                .get()
                .satisfies(token -> {
                    assertThat(token.getPasswordResetTokenId()).isEqualTo(firstToken.getPasswordResetTokenId());
                    assertThat(token.getTokenHash()).isNotEqualTo(firstToken.getTokenHash());
                });
    }

    @Test
    void passwordResetRequestDoesNotRevealMissingUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing-reset-user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void passwordResetRequestRollsBackTokenWhenEmailSendFails() throws Exception {
        userRepository.save(User.builder()
                .email("mail-fail-reset-user@example.com")
                .password("encoded-password")
                .nickname("mail-fail-reset-user")
                .provider("LOCAL")
                .build());
        doThrow(new EmailSendException("failed to send password reset email", new RuntimeException()))
                .when(emailService).sendPasswordResetEmail(eq("mail-fail-reset-user@example.com"), anyString());

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mail-fail-reset-user@example.com"
                                }
                                """))
                .andExpect(status().isBadGateway());

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    private void requestPasswordReset(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
