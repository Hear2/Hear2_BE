package com.hear2.calendar.controller;

import com.hear2.calendar.entity.GoogleCalendarConnection;
import com.hear2.calendar.repository.CalendarEventMemoryLinkRepository;
import com.hear2.calendar.repository.CalendarEventRepository;
import com.hear2.calendar.repository.GoogleCalendarConnectionRepository;
import com.hear2.calendar.repository.GoogleCalendarOAuthStateRepository;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.memory.repository.MemoryAiTagRepository;
import com.hear2.memory.repository.MemoryCommentRepository;
import com.hear2.memory.repository.MemoryRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "google.oauth.client-id=test-client-id",
        "google.oauth.client-secret=test-client-secret",
        "google.calendar.redirect-uri=http://localhost:8080/api/v1/calendar/google/callback"
})
@AutoConfigureMockMvc
class GoogleCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private MemoryAiTagRepository memoryAiTagRepository;

    @Autowired
    private MemoryCommentRepository memoryCommentRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private CalendarEventMemoryLinkRepository calendarEventMemoryLinkRepository;

    @Autowired
    private GoogleCalendarConnectionRepository googleCalendarConnectionRepository;

    @Autowired
    private GoogleCalendarOAuthStateRepository googleCalendarOAuthStateRepository;

    private User user;

    @BeforeEach
    void setUp() {
        googleCalendarOAuthStateRepository.deleteAll();
        googleCalendarConnectionRepository.deleteAll();
        calendarEventMemoryLinkRepository.deleteAll();
        calendarEventRepository.deleteAll();
        memoryCommentRepository.deleteAll();
        memoryAiTagRepository.deleteAll();
        memoryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("google-calendar-user@example.com")
                .password("encoded-password")
                .nickname("예진")
                .provider("LOCAL")
                .build());
        Couple couple = coupleRepository.save(Couple.builder()
                .coupleCode("GOOGLECAL")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());
    }

    @Test
    void connectReturnsGoogleAuthorizationUrlAndStoresState() throws Exception {
        mockMvc.perform(get("/api/v1/calendar/google/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl", containsString("accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(jsonPath("$.data.authorizationUrl", containsString("client_id=test-client-id")))
                .andExpect(jsonPath("$.data.authorizationUrl", containsString("calendar.events")))
                .andExpect(jsonPath("$.data.state").isNotEmpty());

        assertThat(googleCalendarOAuthStateRepository.findAll()).hasSize(1);
    }

    @Test
    void statusAndDisconnectUseAuthenticatedUser() throws Exception {
        googleCalendarConnectionRepository.save(GoogleCalendarConnection.builder()
                .userId(user.getUserId())
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .scopes("https://www.googleapis.com/auth/calendar.events")
                .calendarId("primary")
                .build());

        mockMvc.perform(get("/api/v1/calendar/google/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.calendarId").value("primary"));

        mockMvc.perform(delete("/api/v1/calendar/google/disconnect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        assertThat(googleCalendarConnectionRepository.findByUserId(user.getUserId())).isEmpty();
    }

    @Test
    void callbackIsPublicButRejectsInvalidState() throws Exception {
        mockMvc.perform(get("/api/v1/calendar/google/callback")
                        .param("code", "sample-code")
                        .param("state", "invalid-state"))
                .andExpect(status().isBadRequest());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
