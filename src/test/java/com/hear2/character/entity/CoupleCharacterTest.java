package com.hear2.character.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoupleCharacterTest {

    @Test
    void calculatesStageByExpThresholds() {
        assertThat(characterWithExp(0L).getStage()).isEqualTo(1);
        assertThat(characterWithExp(300L).getStage()).isEqualTo(2);
        assertThat(characterWithExp(900L).getStage()).isEqualTo(3);
        assertThat(characterWithExp(2000L).getStage()).isEqualTo(4);
        assertThat(characterWithExp(3500L).getStage()).isEqualTo(5);
    }

    private CoupleCharacter characterWithExp(long exp) {
        return CoupleCharacter.builder()
                .coupleId(1L)
                .name("콩이")
                .exp(exp)
                .build();
    }
}
