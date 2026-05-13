package com.hear2.auth.controller;

import com.hear2.auth.oauth.KakaoOAuthClient;
import com.hear2.auth.oauth.KakaoOAuthUserInfo;
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
class AuthKakaoOAuthControllerTest {

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
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        reset(kakaoOAuthClient);
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void kakaoOAuthLoginCreatesVerifiedUserAndIssuesTokens() throws Exception {
        when(kakaoOAuthClient.getUserInfo(eq("valid-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10001",
                        "kakao-new-user@example.com",
                        true,
                        "Kakao User",
                        "https://example.com/kakao-profile.png"
                ));

        kakaoLogin("valid-kakao-access-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kakao-new-user@example.com"))
                .andExpect(jsonPath("$.nickname").value("Kakao User"))
                .andExpect(jsonPath("$.profileImage").value("https://example.com/kakao-profile.png"))
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"));

        User user = userRepository.findByEmail("kakao-new-user@example.com").orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getProvider()).isEqualTo("KAKAO");
        assertThat(user.getProviderId()).isEqualTo("10001");
        assertThat(user.getPassword()).isNotBlank();
        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
    }

    @Test
    void kakaoOAuthLoginCreatesUserWithTemporaryEmailWhenEmailIsMissing() throws Exception {
        when(kakaoOAuthClient.getUserInfo(eq("no-email-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10002",
                        "",
                        true,
                        "No Email Kakao User",
                        ""
                ));

        kakaoLogin("no-email-kakao-access-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kakao_10002@kakao.local"))
                .andExpect(jsonPath("$.nickname").value("No Email Kakao User"))
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty());

        User user = userRepository.findByProviderAndProviderId("KAKAO", "10002").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("kakao_10002@kakao.local");
        assertThat(user.getEmailVerified()).isTrue();
    }

    @Test
    void kakaoOAuthLoginUsesEmailPrefixWhenNicknameIsMissing() throws Exception {
        when(kakaoOAuthClient.getUserInfo(eq("no-nickname-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10003",
                        "prefix-kakao-user@example.com",
                        true,
                        "",
                        ""
                ));

        kakaoLogin("no-nickname-kakao-access-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("prefix-kakao-user"));
    }

    @Test
    void kakaoOAuthLoginIssuesTokensForExistingUserWithoutChangingProvider() throws Exception {
        User user = userRepository.save(User.builder()
                .email("existing-kakao-email@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("existing-local-user")
                .provider("LOCAL")
                .build());
        when(kakaoOAuthClient.getUserInfo(eq("existing-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10004",
                        "existing-kakao-email@example.com",
                        true,
                        "Kakao Existing User",
                        "https://example.com/existing-kakao.png"
                ));

        kakaoLogin("existing-kakao-access-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty());

        assertThat(userRepository.findAll()).hasSize(1);
        User loggedInUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(loggedInUser.getEmailVerified()).isTrue();
        assertThat(loggedInUser.getProvider()).isEqualTo("LOCAL");
        assertThat(loggedInUser.getProviderId()).isEqualTo("10004");
        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
    }

    @Test
    void kakaoOAuthLoginFindsExistingKakaoUserByProviderAndProviderIdFirst() throws Exception {
        User user = userRepository.save(User.builder()
                .email("old-kakao-email@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("old-kakao-user")
                .provider("KAKAO")
                .providerId("10005")
                .emailVerified(true)
                .build());
        when(kakaoOAuthClient.getUserInfo(eq("provider-id-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10005",
                        "",
                        true,
                        "Changed Kakao User",
                        ""
                ));

        kakaoLogin("provider-id-kakao-access-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.email").value("old-kakao-email@example.com"))
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty());

        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isPresent();
    }

    @Test
    void kakaoOAuthLoginRejectsUnverifiedKakaoEmail() throws Exception {
        when(kakaoOAuthClient.getUserInfo(eq("unverified-kakao-access-token")))
                .thenReturn(new KakaoOAuthUserInfo(
                        "10006",
                        "unverified-kakao-user@example.com",
                        false,
                        "Unverified Kakao User",
                        ""
                ));

        kakaoLogin("unverified-kakao-access-token")
                .andExpect(status().isForbidden());

        assertThat(userRepository.findAll()).isEmpty();
        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void kakaoOAuthLoginRejectsInvalidKakaoAccessToken() throws Exception {
        when(kakaoOAuthClient.getUserInfo(eq("invalid-kakao-access-token")))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "invalid kakao access token"));

        kakaoLogin("invalid-kakao-access-token")
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findAll()).isEmpty();
        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions kakaoLogin(String accessToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "accessToken": "%s"
                        }
                        """.formatted(accessToken)));
    }
}
