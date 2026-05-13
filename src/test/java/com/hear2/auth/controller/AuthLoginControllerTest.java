package com.hear2.auth.controller;

import com.hear2.auth.repository.EmailVerificationTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginRejectsUnverifiedUserWithoutIssuingTokens() throws Exception {
        userRepository.save(User.builder()
                .email("unverified-login-user@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("unverified-login-user")
                .provider("LOCAL")
                .build());

        login("unverified-login-user@example.com", "password")
                .andExpect(status().isForbidden());

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void loginAllowsVerifiedUserAndKeepsExistingTokenFlow() throws Exception {
        User user = userRepository.save(User.builder()
                .email("verified-login-user@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("verified-login-user")
                .provider("LOCAL")
                .emailVerified(true)
                .build());

        login("verified-login-user@example.com", "password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"));

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> assertThat(token.getUserId()).isEqualTo(user.getUserId()));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)));
    }
}
