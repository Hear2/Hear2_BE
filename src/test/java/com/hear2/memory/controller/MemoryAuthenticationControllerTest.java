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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                  "memo": "로그인 사용자 기준 저장",
                  "takenAt": "2026-05-11T10:30:00"
                }
                """;

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
    void createMemoryAcceptsJsonRequestPartWithoutJsonContentType() throws Exception {
        String requestJson = """
                {
                  "memo": "스웨거 form-data 요청",
                  "takenAt": "2026-05-11T10:30:00"
                }
                """;

        mockMvc.perform(multipart("/api/v1/memories")
                        .file(photoPart())
                        .file(formTextPart(requestJson))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploaderId").value(user.getUserId()))
                .andExpect(jsonPath("$.data.coupleId").value(couple.getCoupleId()))
                .andExpect(jsonPath("$.data.memo").value("스웨거 form-data 요청"));
    }

    @Test
    void getAlbumUsesAuthenticatedUsersCouple() throws Exception {
        String requestJson = """
                {
                  "memo": "앨범 조회 테스트",
                  "takenAt": "2026-05-11T10:30:00"
                }
                """;

        mockMvc.perform(multipart("/api/v1/memories")
                        .file(photoPart())
                        .file(jsonPart(requestJson))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/memories")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].coupleId").value(couple.getCoupleId()))
                .andExpect(jsonPath("$.data[0].memo").value("앨범 조회 테스트"));
    }

    @Test
    void getAlbumRejectsUserWithoutCoupleConnection() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other-memory-user@example.com")
                .password("encoded-password")
                .nickname("other")
                .provider("LOCAL")
                .build());

        mockMvc.perform(get("/api/v1/memories")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser.getUserId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("couple connection not found"));
    }

    @Test
    void createQuickMemoryUsesAuthenticatedUserAndImageUrl() throws Exception {
        mockMvc.perform(post("/api/v1/memory/quick")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "objectKey": "media/memory/1/20260513/photo.jpg",
                                  "lat": 37.5641,
                                  "lng": 126.9244,
                                  "capturedAt": "2026-05-12T14:00:00Z",
                                  "userTags": ["우리둘이", "특별한날"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.imageUrl").value(startsWith("/api/v1/memories/items/")))
                .andExpect(jsonPath("$.data.userTags[0]").value("#우리둘이"))
                .andExpect(jsonPath("$.data.userTags[1]").value("#특별한날"));

        assertThat(memoryRepository.findAll())
                .singleElement()
                .satisfies(memory -> {
                    assertThat(memory.getUploaderId()).isEqualTo(user.getUserId());
                    assertThat(memory.getCoupleId()).isEqualTo(couple.getCoupleId());
                });
    }

    @Test
    void updateQuickMemoryChangesNoteAndUserTags() throws Exception {
        Long memoryId = createQuickMemory();

        mockMvc.perform(patch("/api/v1/memory/quick/{id}", memoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "오늘도 행복한 하루",
                                  "userTags": ["우리둘이"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.note").value("오늘도 행복한 하루"))
                .andExpect(jsonPath("$.data.userTags[0]").value("#우리둘이"));
    }

    @Test
    void calendarAndByDateUseSpecUrls() throws Exception {
        createQuickMemory();

        mockMvc.perform(get("/api/v1/memory/calendar")
                        .param("year", "2026")
                        .param("month", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(5))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-05-12"))
                .andExpect(jsonPath("$.data.days[0].memoryCount").value(1));

        mockMvc.perform(get("/api/v1/memory/by-date")
                        .param("date", "2026-05-12")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].photoUrl").value("https://cdn.example.com/memories/photo.jpg"));

        mockMvc.perform(get("/api/v1/memory/year-ago")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false));
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

    private MockMultipartFile formTextPart(String requestJson) {
        return new MockMultipartFile(
                "request",
                "",
                null,
                requestJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }

    private Long createQuickMemory() throws Exception {
        mockMvc.perform(post("/api/v1/memory/quick")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": "https://cdn.example.com/memories/photo.jpg",
                                  "lat": 37.5641,
                                  "lng": 126.9244,
                                  "capturedAt": "2026-05-12T14:00:00Z",
                                  "userTags": ["우리둘이"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return memoryRepository.findAll().get(0).getId();
    }

    private byte[] onePixelPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        );
    }
}
