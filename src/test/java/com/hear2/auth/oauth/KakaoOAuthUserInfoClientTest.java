package com.hear2.auth.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoOAuthUserInfoClientTest {

    @Test
    void getUserInfoAllowsMissingEmailAndParsesNumericId() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthUserInfoClient client = new KakaoOAuthUserInfoClient(builder.build());

        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 12345,
                          "properties": {
                            "nickname": "Property Nickname",
                            "profile_image": "https://example.com/properties-profile.png"
                          },
                          "kakao_account": {
                            "profile": {
                              "nickname": "Kakao Nickname",
                              "profile_image_url": "https://example.com/kakao-profile.png"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoOAuthUserInfo userInfo = client.getUserInfo("valid-token");

        assertThat(userInfo.providerId()).isEqualTo("12345");
        assertThat(userInfo.email()).isEmpty();
        assertThat(userInfo.emailVerified()).isTrue();
        assertThat(userInfo.nickname()).isEqualTo("Kakao Nickname");
        assertThat(userInfo.profileImage()).isEqualTo("https://example.com/kakao-profile.png");
        server.verify();
    }

    @Test
    void getUserInfoReturnsUnauthorizedOnlyForKakaoApiFailure() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthUserInfoClient client = new KakaoOAuthUserInfoClient(builder.build());

        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getUserInfo("invalid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException statusException = (ResponseStatusException) exception;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(statusException.getReason()).isEqualTo("invalid kakao access token");
                });

        server.verify();
    }
}
