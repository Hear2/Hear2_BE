package com.hear2.location.controller;

import com.hear2.global.security.JwtProvider;
import com.hear2.location.repository.LocationShareSettingRepository;
import com.hear2.location.repository.UserLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUp() {
        userLocationRepository.deleteAll();
        locationShareSettingRepository.deleteAll();
    }

    @Test
    void userCanEnableSharingUpdateLocationAndPartnerCanReadIt() throws Exception {
        mockMvc.perform(put("/api/v1/locations/share-status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(put("/api/v1/locations/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "latitude": 37.2221,
                                  "longitude": 127.1875,
                                  "accuracyMeters": 20,
                                  "placeName": "Myeongji Nature Campus",
                                  "addressName": "116 Myeongji-ro, Yongin",
                                  "recordedAt": "2026-05-09T14:30:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.latitude").value(37.2221))
                .andExpect(jsonPath("$.data.longitude").value(127.1875));

        mockMvc.perform(get("/api/v1/locations/couples/1/partners/20")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shared").value(true))
                .andExpect(jsonPath("$.data.location.userId").value(10))
                .andExpect(jsonPath("$.data.location.placeName").value("Myeongji Nature Campus"));
    }

    @Test
    void currentLocationUpdateIsRejectedWhenSharingIsDisabled() throws Exception {
        mockMvc.perform(put("/api/v1/locations/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "latitude": 37.2221,
                                  "longitude": 127.1875
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("location sharing is disabled"));
    }

    @Test
    void partnerLocationIsHiddenAfterUserTurnsSharingOff() throws Exception {
        mockMvc.perform(put("/api/v1/locations/share-status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/locations/current")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "latitude": 37.2221,
                                  "longitude": 127.1875
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/locations/share-status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coupleId": 1,
                                  "userId": 10,
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/locations/couples/1/partners/20")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shared").value(false))
                .andExpect(jsonPath("$.data.location").doesNotExist());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
