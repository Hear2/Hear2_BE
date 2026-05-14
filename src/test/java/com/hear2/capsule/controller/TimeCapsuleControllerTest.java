package com.hear2.capsule.controller;

import com.hear2.capsule.entity.TimeCapsule;
import com.hear2.capsule.entity.TimeCapsuleCoverStyle;
import com.hear2.capsule.entity.TimeCapsuleStatus;
import com.hear2.capsule.repository.TimeCapsuleLetterRepository;
import com.hear2.capsule.repository.TimeCapsuleMediaRepository;
import com.hear2.capsule.repository.TimeCapsuleRepository;
import com.hear2.capsule.repository.TimeCapsuleSnapshotRepository;
import com.hear2.capsule.service.TimeCapsuleService;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeCapsuleControllerTest {

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
    private TimeCapsuleRepository timeCapsuleRepository;

    @Autowired
    private TimeCapsuleLetterRepository timeCapsuleLetterRepository;

    @Autowired
    private TimeCapsuleMediaRepository timeCapsuleMediaRepository;

    @Autowired
    private TimeCapsuleSnapshotRepository timeCapsuleSnapshotRepository;

    @Autowired
    private TimeCapsuleService timeCapsuleService;

    private User user;
    private Couple couple;

    @BeforeEach
    void setUp() {
        timeCapsuleSnapshotRepository.deleteAll();
        timeCapsuleMediaRepository.deleteAll();
        timeCapsuleLetterRepository.deleteAll();
        timeCapsuleRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("capsule-user@example.com")
                .password("encoded-password")
                .nickname("capsule-user")
                .provider("LOCAL")
                .build());

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("CAPSULE1")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());
    }

    @Test
    void createCapsuleUsesAuthenticatedUsersCouple() throws Exception {
        mockMvc.perform(post("/api/v1/capsule")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "1주년 기념 캡슐",
                                  "coverStyle": "LETTER",
                                  "openAt": "2027-05-16T00:00:00Z",
                                  "letter": "지금 이 순간이 너무 행복해 사랑해",
                                  "photoObjectKeys": ["media/capsule/7/20260513/photo.jpg"],
                                  "options": {
                                    "blindOthersAnswer": true,
                                    "notifyBeforeOpen": true,
                                    "confettiOnOpen": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("1주년 기념 캡슐"))
                .andExpect(jsonPath("$.data.status").value("SEALED"))
                .andExpect(jsonPath("$.data.photos").isArray());

        assertThat(timeCapsuleRepository.findAll())
                .singleElement()
                .satisfies(capsule -> assertThat(capsule.getCoupleId()).isEqualTo(couple.getCoupleId()));
    }

    @Test
    void createCapsuleAllowsNoPhotos() throws Exception {
        mockMvc.perform(post("/api/v1/capsule")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "사진 없는 캡슐",
                                  "coverStyle": "LETTER",
                                  "openAt": "2027-05-16T00:00:00Z",
                                  "letter": "사진 없이 편지만 봉인할게",
                                  "photoObjectKeys": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("사진 없는 캡슐"))
                .andExpect(jsonPath("$.data.status").value("SEALED"))
                .andExpect(jsonPath("$.data.photos").isArray());
    }

    @Test
    void shareCardRejectsSealedCapsule() throws Exception {
        mockMvc.perform(post("/api/v1/capsule")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "공유 전 캡슐",
                                  "coverStyle": "LETTER",
                                  "openAt": "2027-05-16T00:00:00Z",
                                  "letter": "아직 열면 안 돼"
                                }
                                """))
                .andExpect(status().isOk());

        Long capsuleId = timeCapsuleRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/v1/capsule/{capsuleId}/share-card", capsuleId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void openExpiredCapsulesOpensDueSealedCapsules() {
        LocalDateTime now = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime();
        TimeCapsule capsule = timeCapsuleRepository.save(TimeCapsule.builder()
                .coupleId(couple.getCoupleId())
                .name("곧 열리는 캡슐")
                .coverStyle(TimeCapsuleCoverStyle.LETTER)
                .openAt(now.minusMinutes(1))
                .sealedAt(now.minusDays(1))
                .status(TimeCapsuleStatus.SEALED)
                .build());

        timeCapsuleService.openExpiredCapsules();

        TimeCapsule openedCapsule = timeCapsuleRepository.findById(capsule.getId()).orElseThrow();
        assertThat(openedCapsule.getStatus()).isEqualTo(TimeCapsuleStatus.OPEN);
        assertThat(openedCapsule.getOpenedAt()).isNotNull();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
