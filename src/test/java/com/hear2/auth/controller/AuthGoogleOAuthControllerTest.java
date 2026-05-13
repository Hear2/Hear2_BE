package com.hear2.auth.controller;

import com.hear2.auth.oauth.GoogleOAuthClient;
import com.hear2.auth.oauth.GoogleOAuthUserInfo;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthGoogleOAuthControllerTest {

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

    @MockitoBean
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUp() {
        reset(googleOAuthClient);
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void googleOAuthLoginCreatesVerifiedUserAndIssuesTokens() throws Exception {
        when(googleOAuthClient.verifyIdToken(eq("valid-google-id-token")))
                .thenReturn(new GoogleOAuthUserInfo(
                        "google-new-user@example.com",
                        true,
                        "Google User",
                        "https://example.com/profile.png"
                ));

        googleLogin("valid-google-id-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google-new-user@example.com"))
                .andExpect(jsonPath("$.nickname").value("Google User"))
                .andExpect(jsonPath("$.profileImage").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"));

        User user = userRepository.findByEmail("google-new-user@example.com").orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getProvider()).isEqualTo("GOOGLE");
        assertThat(user.getPassword()).isNotBlank();
        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
    }

    @Test
    void googleOAuthLoginUsesEmailPrefixWhenNameIsMissing() throws Exception {
        when(googleOAuthClient.verifyIdToken(eq("no-name-google-id-token")))
                .thenReturn(new GoogleOAuthUserInfo(
                        "prefix-user@example.com",
                        true,
                        "",
                        ""
                ));

        googleLogin("no-name-google-id-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("prefix-user"));
    }

    @Test
    void googleOAuthLoginIssuesTokensForExistingUserWithoutChangingProvider() throws Exception {
        User user = userRepository.save(User.builder()
                .email("existing-local-user@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("existing-local-user")
                .provider("LOCAL")
                .build());
        when(googleOAuthClient.verifyIdToken(eq("existing-google-id-token")))
                .thenReturn(new GoogleOAuthUserInfo(
                        "existing-local-user@example.com",
                        true,
                        "Google Existing User",
                        "https://example.com/existing.png"
                ));

        googleLogin("existing-google-id-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty());

        assertThat(userRepository.findAll()).hasSize(1);
        User loggedInUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(loggedInUser.getEmailVerified()).isTrue();
        assertThat(loggedInUser.getProvider()).isEqualTo("LOCAL");
        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
    }

    @Test
    void googleOAuthLoginRejectsUnverifiedGoogleEmail() throws Exception {
        when(googleOAuthClient.verifyIdToken(eq("unverified-google-id-token")))
                .thenReturn(new GoogleOAuthUserInfo(
                        "unverified-google-user@example.com",
                        false,
                        "Unverified User",
                        ""
                ));

        googleLogin("unverified-google-id-token")
                .andExpect(status().isForbidden());

        assertThat(userRepository.findAll()).isEmpty();
        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void googleOAuthLoginRejectsInvalidGoogleIdToken() throws Exception {
        when(googleOAuthClient.verifyIdToken(eq("invalid-google-id-token")))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "invalid google id token"));

        googleLogin("invalid-google-id-token")
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findAll()).isEmpty();
        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions googleLogin(String idToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "idToken": "%s"
                        }
                        """.formatted(idToken)));
    }
}
