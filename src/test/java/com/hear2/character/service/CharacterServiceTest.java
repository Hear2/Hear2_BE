package com.hear2.character.service;

import com.hear2.character.dto.CharacterExpGrantResult;
import com.hear2.character.entity.CoupleCharacter;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.repository.CoupleCharacterRepository;
import com.hear2.character.support.CharacterExpSourceType;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CharacterServiceTest {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleCharacterRepository coupleCharacterRepository;

    @Autowired
    private CharacterExpHistoryRepository characterExpHistoryRepository;

    private Couple couple;

    @BeforeEach
    void setUp() {
        characterExpHistoryRepository.deleteAll();
        coupleCharacterRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("EXP00001")
                .build());
    }

    @Test
    void grantExpCreatesCharacterAndIncreasesExp() {
        CharacterExpGrantResult result = characterService.grantExp(
                couple.getCoupleId(),
                CharacterExpSourceType.ATTENDANCE,
                "attendance-1",
                50L
        );

        assertThat(result.granted()).isTrue();
        assertThat(result.requestedExp()).isEqualTo(50L);
        assertThat(result.grantedExp()).isEqualTo(50L);
        assertThat(result.duplicated()).isFalse();
        assertThat(result.limited()).isFalse();

        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(50L));
        assertThat(characterExpHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    void grantExpPreventsDuplicateSourceReward() {
        characterService.grantExp(couple.getCoupleId(), CharacterExpSourceType.MEMORY, "memory-1", 50L);

        CharacterExpGrantResult duplicated = characterService.grantExp(
                couple.getCoupleId(),
                CharacterExpSourceType.MEMORY,
                "memory-1",
                50L
        );

        assertThat(duplicated.granted()).isFalse();
        assertThat(duplicated.grantedExp()).isZero();
        assertThat(duplicated.duplicated()).isTrue();
        assertThat(duplicated.limited()).isFalse();
        assertThat(characterExpHistoryRepository.count()).isEqualTo(1);
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(50L));
    }

    @Test
    void grantExpCapsSingleRequestToDailyLimitAndCreatesCharacter() {
        CharacterExpGrantResult result = characterService.grantExp(
                couple.getCoupleId(),
                CharacterExpSourceType.DAILY_QNA,
                "daily-qna-1",
                200L
        );

        assertThat(result.granted()).isTrue();
        assertThat(result.requestedExp()).isEqualTo(200L);
        assertThat(result.grantedExp()).isEqualTo(150L);
        assertThat(result.limited()).isTrue();
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(150L));
        assertThat(dailyExpSum()).isEqualTo(150L);
    }

    @Test
    void grantExpAppliesRemainingDailyLimitOnly() {
        characterService.grantExp(couple.getCoupleId(), CharacterExpSourceType.CHAT, "message-1", 140L);

        CharacterExpGrantResult result = characterService.grantExp(
                couple.getCoupleId(),
                CharacterExpSourceType.CHAT,
                "message-2",
                20L
        );

        assertThat(result.granted()).isTrue();
        assertThat(result.requestedExp()).isEqualTo(20L);
        assertThat(result.grantedExp()).isEqualTo(10L);
        assertThat(result.limited()).isTrue();
        assertThat(characterExpHistoryRepository.count()).isEqualTo(2);
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(150L));
    }

    @Test
    void grantExpDoesNotSaveHistoryWhenDailyLimitAlreadyReached() {
        coupleCharacterRepository.save(CoupleCharacter.builder()
                .coupleId(couple.getCoupleId())
                .name("existing")
                .exp(0L)
                .build());
        characterService.grantExp(couple.getCoupleId(), CharacterExpSourceType.CHAT, "message-1", 150L);

        CharacterExpGrantResult result = characterService.grantExp(
                couple.getCoupleId(),
                CharacterExpSourceType.CHAT,
                "message-2",
                10L
        );

        assertThat(result.granted()).isFalse();
        assertThat(result.grantedExp()).isZero();
        assertThat(result.duplicated()).isFalse();
        assertThat(result.limited()).isTrue();
        assertThat(characterExpHistoryRepository.count()).isEqualTo(1);
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(150L));
    }

    private long dailyExpSum() {
        return characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDate(
                couple.getCoupleId(),
                LocalDate.now()
        );
    }
}
