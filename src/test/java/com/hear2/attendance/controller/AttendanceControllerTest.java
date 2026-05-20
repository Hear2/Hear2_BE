package com.hear2.attendance.controller;

import com.hear2.attendance.repository.AttendanceCheckRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttendanceControllerTest {

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
    private AttendanceCheckRepository attendanceCheckRepository;

    private User user;
    private User partner;
    private Couple couple;

    @BeforeEach
    void setUp() {
        attendanceCheckRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("attendance-user@example.com")
                .password("encoded-password")
                .nickname("attendance-user")
                .provider("LOCAL")
                .build());

        partner = userRepository.save(User.builder()
                .email("attendance-partner@example.com")
                .password("encoded-password")
                .nickname("attendance-partner")
                .provider("LOCAL")
                .build());

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("ATTEND01")
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

    @Test
    void checkTodayCreatesOneAttendancePerUserPerDate() throws Exception {
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/api/v1/attendance/check")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceDate").value(today))
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.alreadyChecked").value(false))
                .andExpect(jsonPath("$.coupleBothChecked").value(false));

        mockMvc.perform(post("/api/v1/attendance/check")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceDate").value(today))
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.alreadyChecked").value(true))
                .andExpect(jsonPath("$.coupleBothChecked").value(false));

        assertThat(attendanceCheckRepository.count()).isEqualTo(1);
    }

    @Test
    void todayReturnsPartnerAndBothCheckedStatus() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/check")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/attendance/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChecked").value(false))
                .andExpect(jsonPath("$.partnerChecked").value(true))
                .andExpect(jsonPath("$.coupleBothChecked").value(false));

        mockMvc.perform(post("/api/v1/attendance/check")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupleBothChecked").value(true));

        mockMvc.perform(get("/api/v1/attendance/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChecked").value(true))
                .andExpect(jsonPath("$.partnerChecked").value(true))
                .andExpect(jsonPath("$.coupleBothChecked").value(true));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
