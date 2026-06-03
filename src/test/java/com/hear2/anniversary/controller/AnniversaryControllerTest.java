package com.hear2.anniversary.controller;

import com.hear2.anniversary.repository.AnniversaryRepository;
import com.hear2.anniversary.repository.HiddenAutoAnniversaryRepository;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnniversaryControllerTest {

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
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private HiddenAutoAnniversaryRepository hiddenAutoAnniversaryRepository;

    private User user;
    private User partner;
    private Couple couple;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user = userRepository.save(User.builder()
                .email("anniversary-user@example.com")
                .password("encoded-password")
                .nickname("예진")
                .provider("LOCAL")
                .build());
        partner = userRepository.save(User.builder()
                .email("anniversary-partner@example.com")
                .password("encoded-password")
                .nickname("지호")
                .provider("LOCAL")
                .build());
        couple = coupleRepository.save(Couple.builder()
                .coupleCode("ANNIV")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(partner.getUserId())
                .role("MEMBER")
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void updateStartDateReturnsAutomaticAnniversaries() throws Exception {
        mockMvc.perform(put("/api/v1/anniversaries/start-date")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2024-12-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value("2024-12-20"))
                .andExpect(jsonPath("$.data.daysTogether").isNumber())
                .andExpect(jsonPath("$.data.anniversaries[*].autoKey", hasItem("LOVE_DAY")))
                .andExpect(jsonPath("$.data.anniversaries[*].autoKey", hasItem("DAY_100")))
                .andExpect(jsonPath("$.data.anniversaries[*].autoKey", hasItem("YEAR_1")));

        assertThat(coupleRepository.findById(couple.getCoupleId()))
                .get()
                .extracting(Couple::getStartDate)
                .isEqualTo(java.time.LocalDate.of(2024, 12, 20));
    }

    @Test
    void createUpdateAndDeleteCustomAnniversary() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/anniversaries")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "예진 생일",
                                  "type": "BIRTHDAY",
                                  "date": "2026-07-14",
                                  "ddayType": "D_MINUS",
                                  "repeatYearly": true,
                                  "shared": true,
                                  "icon": "CAKE",
                                  "color": "#FFB75E",
                                  "notifyDays": [7, 1, 0]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("예진 생일"))
                .andExpect(jsonPath("$.data.type").value("BIRTHDAY"))
                .andExpect(jsonPath("$.data.repeatYearly").value(true))
                .andExpect(jsonPath("$.data.notifyDays[*]", hasItem(7)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number anniversaryId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(patch("/api/v1/anniversaries/{anniversaryId}", anniversaryId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "예진 생일파티",
                                  "type": "BIRTHDAY",
                                  "date": "2026-07-14",
                                  "ddayType": "D_MINUS",
                                  "repeatYearly": true,
                                  "shared": true,
                                  "icon": "CAKE",
                                  "color": "#FF4D8D",
                                  "notifyDays": [3, 0]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("예진 생일파티"))
                .andExpect(jsonPath("$.data.notifyDays[*]", hasItem(3)));

        mockMvc.perform(delete("/api/v1/anniversaries/{anniversaryId}", anniversaryId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        assertThat(anniversaryRepository.findById(anniversaryId.longValue())).isEmpty();
    }

    @Test
    void personalAnniversaryIsVisibleOnlyToCreator() throws Exception {
        mockMvc.perform(post("/api/v1/anniversaries")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "개인 메모",
                                  "type": "CUSTOM",
                                  "date": "2026-08-01",
                                  "shared": false
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/anniversaries")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anniversaries[*].title", hasItem("개인 메모")));

        mockMvc.perform(get("/api/v1/anniversaries")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anniversaries[*].title", not(hasItem("개인 메모"))));
    }

    @Test
    void hideAndShowAutomaticAnniversary() throws Exception {
        couple.updateStartDate(java.time.LocalDate.of(2024, 12, 20));
        coupleRepository.save(couple);

        mockMvc.perform(post("/api/v1/anniversaries/auto/{autoKey}/hide", "DAY_100")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anniversaries[*].autoKey", not(hasItem("DAY_100"))));

        mockMvc.perform(delete("/api/v1/anniversaries/auto/{autoKey}/hide", "DAY_100")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anniversaries[*].autoKey", hasItem("DAY_100")));
    }

    private void cleanDatabase() {
        hiddenAutoAnniversaryRepository.deleteAll();
        anniversaryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
