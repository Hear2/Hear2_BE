package com.hear2.character.controller;

import com.hear2.character.repository.CoupleCharacterRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CharacterControllerTest {

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
    private CoupleCharacterRepository coupleCharacterRepository;

    private User user;
    private Couple couple;

    @BeforeEach
    void setUp() {
        coupleCharacterRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("character-user@example.com")
                .password("encoded-password")
                .nickname("character-user")
                .provider("LOCAL")
                .build());

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("CHAR0001")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());
    }

    @Test
    void getCharacterCreatesDefaultCharacterWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/character")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterName").value("콩이"))
                .andExpect(jsonPath("$.exp").value(0))
                .andExpect(jsonPath("$.stage").value(1));

        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> {
                    assertThat(character.getName()).isEqualTo("콩이");
                    assertThat(character.getExp()).isZero();
                });
    }

    @Test
    void getCharacterReturnsExistingCharacterWithoutDuplicateCreation() throws Exception {
        coupleCharacterRepository.save(com.hear2.character.entity.CoupleCharacter.builder()
                .coupleId(couple.getCoupleId())
                .name("기존이")
                .exp(900L)
                .build());

        mockMvc.perform(get("/api/v1/character")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterName").value("기존이"))
                .andExpect(jsonPath("$.exp").value(900))
                .andExpect(jsonPath("$.stage").value(3));

        assertThat(coupleCharacterRepository.count()).isEqualTo(1);
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
