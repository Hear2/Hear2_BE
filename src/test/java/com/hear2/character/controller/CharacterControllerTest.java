package com.hear2.character.controller;

import com.hear2.character.entity.CharacterExpHistory;
import com.hear2.character.repository.CoupleCharacterRepository;
import com.hear2.character.entity.CoupleCharacter;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.support.CharacterExpSourceType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private CharacterExpHistoryRepository characterExpHistoryRepository;

    private User user;
    private Couple couple;

    @BeforeEach
    void setUp() {
        characterExpHistoryRepository.deleteAll();
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

    @Test
    void updateCharacterNameChangesNameAndKeepsExpAndStage() throws Exception {
        CoupleCharacter character = coupleCharacterRepository.save(CoupleCharacter.builder()
                .coupleId(couple.getCoupleId())
                .name("Before")
                .exp(900L)
                .build());

        mockMvc.perform(patch("/api/v1/character/name")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dubu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterName").value("Dubu"))
                .andExpect(jsonPath("$.exp").value(900))
                .andExpect(jsonPath("$.stage").value(3));

        assertThat(coupleCharacterRepository.count()).isEqualTo(1);
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(updated -> {
                    assertThat(updated.getCharacterId()).isEqualTo(character.getCharacterId());
                    assertThat(updated.getName()).isEqualTo("Dubu");
                    assertThat(updated.getExp()).isEqualTo(900L);
                    assertThat(updated.getStage()).isEqualTo(3);
                });
    }

    @Test
    void updateCharacterNameRejectsBlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/character/name")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCharacterNameRejectsNameOverTenCharacters() throws Exception {
        mockMvc.perform(patch("/api/v1/character/name")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"abcdefghijk\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTodayExpHistoryReturnsTodayItemsOrderedByCreatedAt() throws Exception {
        Couple otherCouple = coupleRepository.save(Couple.builder()
                .coupleCode("OTHER001")
                .build());
        LocalDate today = LocalDate.now();

        saveHistory(couple.getCoupleId(), CharacterExpSourceType.MEMORY, "MEMORY:1", 20L,
                today, LocalDateTime.of(2026, 5, 23, 12, 0));
        saveHistory(couple.getCoupleId(), CharacterExpSourceType.ATTENDANCE, "ATTENDANCE:1", 10L,
                today, LocalDateTime.of(2026, 5, 23, 10, 0));
        saveHistory(couple.getCoupleId(), CharacterExpSourceType.CHAT, "CHAT:1", 1L,
                today, LocalDateTime.of(2026, 5, 23, 12, 10));
        saveHistory(couple.getCoupleId(), CharacterExpSourceType.DAILY_QNA, "DAILY_QNA:YESTERDAY", 30L,
                today.minusDays(1), LocalDateTime.of(2026, 5, 22, 9, 0));
        saveHistory(otherCouple.getCoupleId(), CharacterExpSourceType.CHAT, "CHAT:OTHER", 99L,
                today, LocalDateTime.of(2026, 5, 23, 9, 0));

        mockMvc.perform(get("/api/v1/character/exp/history/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExp").value(31))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].sourceType").value("ATTENDANCE"))
                .andExpect(jsonPath("$.items[0].expAmount").value(10))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-05-23T10:00:00"))
                .andExpect(jsonPath("$.items[1].sourceType").value("MEMORY"))
                .andExpect(jsonPath("$.items[1].expAmount").value(20))
                .andExpect(jsonPath("$.items[2].sourceType").value("CHAT"))
                .andExpect(jsonPath("$.items[2].expAmount").value(1));
    }

    @Test
    void getTodayExpHistoryReturnsEmptyItemsWhenNoHistory() throws Exception {
        mockMvc.perform(get("/api/v1/character/exp/history/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExp").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }

    private void saveHistory(
            Long coupleId,
            CharacterExpSourceType sourceType,
            String sourceId,
            Long expAmount,
            LocalDate earnedDate,
            LocalDateTime createdAt
    ) {
        characterExpHistoryRepository.save(CharacterExpHistory.builder()
                .coupleId(coupleId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .expAmount(expAmount)
                .earnedDate(earnedDate)
                .createdAt(createdAt)
                .build());
    }
}
