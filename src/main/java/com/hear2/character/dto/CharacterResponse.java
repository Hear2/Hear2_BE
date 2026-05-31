package com.hear2.character.dto;

import com.hear2.character.entity.CoupleCharacter;

public record CharacterResponse(
        String characterName,
        long exp,
        int stage
) {

    public static CharacterResponse from(CoupleCharacter character) {
        return new CharacterResponse(
                character.getName(),
                character.getExp(),
                character.getStage()
        );
    }
}
