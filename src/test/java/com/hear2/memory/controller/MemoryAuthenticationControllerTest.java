package com.hear2.memory.controller;

import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.memory.repository.MemoryRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemoryAuthenticationControllerTest {

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

    private User user;
    private Couple couple;

    @BeforeEach
    void setUp() {
        memoryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("memory-user@example.com")
                .password("encoded-password")
                .nickname("memory-user")
                .provider("LOCAL")
                .build());

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("MEMORY01")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());
    }

    @Test
    void createMemoryUsesAuthenticatedUserAsUploader() throws Exception {
        String requestJson = """
                {
                  "coupleId": %d,
                  "uploaderId": 999999,
                  "memo": "로그인 사용자 기준 저장",
                  "takenAt": "2026-05-11T10:30:00"
                }
                """.formatted(couple.getCoupleId());

        mockMvc.perform(multipart("/api/v1/memories")
                        .file(photoPart())
                        .file(jsonPart(requestJson))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploaderId").value(user.getUserId()))
                .andExpect(jsonPath("$.data.coupleId").value(couple.getCoupleId()))
                .andExpect(jsonPath("$.data.memo").value("로그인 사용자 기준 저장"));

        assertThat(memoryRepository.findAll())
                .singleElement()
                .satisfies(memory -> assertThat(memory.getUploaderId()).isEqualTo(user.getUserId()));
    }

    @Test
    void getAlbumRejectsUserWhoIsNotCoupleMember() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other-memory-user@example.com")
                .password("encoded-password")
                .nickname("other")
                .provider("LOCAL")
                .build());

        mockMvc.perform(get("/api/v1/memories/couples/{coupleId}", couple.getCoupleId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("user is not a member of this couple"));
    }

    private MockMultipartFile photoPart() {
        return new MockMultipartFile(
                "photo",
                "memory.png",
                MediaType.IMAGE_PNG_VALUE,
                onePixelPng()
        );
    }

    private MockMultipartFile jsonPart(String requestJson) {
        return new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                requestJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }

    private byte[] onePixelPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        );
    }
}
