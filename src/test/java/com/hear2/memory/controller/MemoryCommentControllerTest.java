package com.hear2.memory.controller;

import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.memory.entity.Memory;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemoryCommentControllerTest {

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

    private User user;
    private User partner;
    private Couple couple;
    private Memory memory;

    @BeforeEach
    void setUp() {
        memoryCommentRepository.deleteAll();
        memoryAiTagRepository.deleteAll();
        memoryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("memory-comment-user@example.com")
                .password("encoded-password")
                .nickname("예진")
                .provider("LOCAL")
                .build());
        partner = userRepository.save(User.builder()
                .email("memory-comment-partner@example.com")
                .password("encoded-password")
                .nickname("지호")
                .provider("LOCAL")
                .build());
        couple = coupleRepository.save(Couple.builder()
                .coupleCode("MEMCOMMENT")
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

        memory = memoryRepository.save(Memory.builder()
                .coupleId(couple.getCoupleId())
                .uploaderId(user.getUserId())
                .storedPhotoPath("s3://memory/comment-test.jpg")
                .originalFileName("comment-test.jpg")
                .photoContentType("image/jpeg")
                .photoSize(1024L)
                .memo("서울숲 벚꽃")
                .memoryDate(LocalDate.of(2026, 4, 12))
                .build());
    }

    @Test
    void createAndReadMemoryComments() throws Exception {
        mockMvc.perform(post("/api/v1/memories/items/{memoryId}/comments", memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "이날 같이 가자고 한 거 진짜 잘했다"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memoryId").value(memory.getId()))
                .andExpect(jsonPath("$.data.writerId").value(user.getUserId()))
                .andExpect(jsonPath("$.data.writerNickname").value("예진"))
                .andExpect(jsonPath("$.data.content").value("이날 같이 가자고 한 거 진짜 잘했다"));

        mockMvc.perform(get("/api/v1/memories/items/{memoryId}/comments", memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].writerNickname").value("예진"))
                .andExpect(jsonPath("$.data[0].content").value("이날 같이 가자고 한 거 진짜 잘했다"));

        mockMvc.perform(get("/api/v1/memories/items/{memoryId}", memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].content").value("이날 같이 가자고 한 거 진짜 잘했다"));
    }

    @Test
    void deleteCommentAllowsOnlyWriter() throws Exception {
        Long commentId = createComment(user.getUserId());

        mockMvc.perform(delete("/api/v1/memories/items/{memoryId}/comments/{commentId}", memory.getId(), commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/memories/items/{memoryId}/comments/{commentId}", memory.getId(), commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        assertThat(memoryCommentRepository.findAll()).isEmpty();
    }

    private Long createComment(Long userId) throws Exception {
        mockMvc.perform(post("/api/v1/memories/items/{memoryId}/comments", memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "다음에도 여기 가자"
                                }
                                """))
                .andExpect(status().isOk());

        return memoryCommentRepository.findAll().get(0).getId();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
