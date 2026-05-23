package com.hear2.couple.controller;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleCodeRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CoupleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleCodeRepository coupleCodeRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        coupleMemberRepository.deleteAll();
        coupleCodeRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        userA = saveUser("couple-a@example.com");
        userB = saveUser("couple-b@example.com");
        userC = saveUser("couple-c@example.com");
    }

    @Test
    void createCodeDoesNotCreateCoupleConnection() throws Exception {
        mockMvc.perform(post("/api/v1/couples/code")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.coupleId").doesNotExist())
                .andExpect(jsonPath("$.coupleCode").isString())
                .andExpect(jsonPath("$.memberCount").value(0));

        assertThat(coupleRepository.count()).isZero();
        assertThat(coupleMemberRepository.count()).isZero();
        assertThat(coupleCodeRepository.findByIssuerUserIdAndUsedAtIsNull(userA.getUserId()))
                .hasSize(1);
    }

    @Test
    void connectWithPartnerCodeCreatesCoupleForBothUsers() throws Exception {
        String userACode = createCode(userA);
        String userBCode = createCode(userB);

        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody(userBCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.coupleCode").value(userBCode))
                .andExpect(jsonPath("$.memberCount").value(2));

        List<CoupleMember> members = coupleMemberRepository.findAll();
        assertThat(members).hasSize(2);
        assertThat(members.stream().map(CoupleMember::getCoupleId).distinct()).hasSize(1);
        assertThat(coupleMemberRepository.findByUserId(userA.getUserId()).orElseThrow().getCoupleId())
                .isEqualTo(coupleMemberRepository.findByUserId(userB.getUserId()).orElseThrow().getCoupleId());
        assertThat(coupleRepository.count()).isEqualTo(1);
        assertThat(coupleCodeRepository.findByIssuerUserIdAndUsedAtIsNull(userA.getUserId())).isEmpty();
        assertThat(coupleCodeRepository.findByIssuerUserIdAndUsedAtIsNull(userB.getUserId())).isEmpty();
        assertThat(userACode).isNotBlank();
    }

    @Test
    void connectRejectsOwnCode() throws Exception {
        String code = createCode(userA);

        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody(code)))
                .andExpect(status().isBadRequest());

        assertThat(coupleRepository.count()).isZero();
        assertThat(coupleMemberRepository.count()).isZero();
    }

    @Test
    void connectRejectsAlreadyConnectedUser() throws Exception {
        String userBCode = createCode(userB);
        connect(userA, userBCode);
        String userCCode = createCode(userC);

        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody(userCCode)))
                .andExpect(status().isConflict());
    }

    @Test
    void connectRejectsUsedCode() throws Exception {
        String userBCode = createCode(userB);
        connect(userA, userBCode);

        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userC.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody(userBCode)))
                .andExpect(status().isConflict());
    }

    @Test
    void connectRejectsUnknownCode() throws Exception {
        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody("NOPE1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusReturnsPendingCodeUntilConnected() throws Exception {
        String code = createCode(userA);

        mockMvc.perform(get("/api/v1/couples/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userA.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.coupleId").doesNotExist())
                .andExpect(jsonPath("$.coupleCode").value(code))
                .andExpect(jsonPath("$.memberCount").value(0));
    }

    private String createCode(User user) throws Exception {
        mockMvc.perform(post("/api/v1/couples/code")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        return coupleCodeRepository
                .findTopByIssuerUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getUserId())
                .orElseThrow()
                .getCode();
    }

    private void connect(User user, String code) throws Exception {
        mockMvc.perform(post("/api/v1/couples/connect")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectBody(code)))
                .andExpect(status().isOk());
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .nickname(email)
                .provider("LOCAL")
                .build());
    }

    private String connectBody(String code) {
        return "{\"coupleCode\":\"" + code + "\"}";
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
