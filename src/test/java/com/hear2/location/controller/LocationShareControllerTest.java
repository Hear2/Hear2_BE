package com.hear2.location.controller;

import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.location.repository.LocationShareSettingRepository;
import com.hear2.location.repository.UserLocationRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocationShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private LocationShareSettingRepository locationShareSettingRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private UserRepository userRepository;

    private Couple couple;
    private User me;
    private User partner;

    @BeforeEach
    void setUp() {
        userLocationRepository.deleteAll();
        locationShareSettingRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        me = saveUser("location-controller-me@example.com");
        partner = saveUser("location-controller-partner@example.com");
        couple = coupleRepository.save(Couple.builder()
                .coupleCode("LOC002")
                .build());

        saveMember(couple.getCoupleId(), me.getUserId(), "OWNER");
        saveMember(couple.getCoupleId(), partner.getUserId(), "PARTNER");
    }

    @Test
    void userCanEnableSharingUpdateLocationAndPartnerCanReadIt() throws Exception {
        mockMvc.perform(put("/api/v1/location/sharing")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(me.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(put("/api/v1/location/sharing")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(me.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lat": 37.2221,
                                  "lng": 127.1875,
                                  "accuracy": 20,
                                  "capturedAt": "2026-05-09T14:30:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.savedAt").exists());

        mockMvc.perform(get("/api/v1/couple/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partner.userId").value(me.getUserId()))
                .andExpect(jsonPath("$.data.partner.lat").value(37.2221))
                .andExpect(jsonPath("$.data.partner.lng").value(127.1875));
    }

    @Test
    void currentLocationUpdateIsRejectedWhenSharingIsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(me.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lat": 37.2221,
                                  "lng": 127.1875
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("location sharing is disabled"));
    }

    @Test
    void partnerLocationIsHiddenAfterUserTurnsSharingOff() throws Exception {
        enableSharing(me.getUserId());
        enableSharing(partner.getUserId());

        mockMvc.perform(post("/api/v1/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(me.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lat": 37.2221,
                                  "lng": 127.1875
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/location/sharing")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(me.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/couple/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partner").doesNotExist());
    }

    @Test
    void coupleLocationRequiresOwnSharingEnabled() throws Exception {
        enableSharing(me.getUserId());

        mockMvc.perform(get("/api/v1/couple/location")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("location sharing is disabled"));
    }

    private void enableSharing(Long userId) throws Exception {
        mockMvc.perform(put("/api/v1/location/sharing")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .nickname(email)
                .provider("LOCAL")
                .emailVerified(true)
                .build());
    }

    private void saveMember(Long coupleId, Long userId, String role) {
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(coupleId)
                .userId(userId)
                .role(role)
                .build());
    }
}
