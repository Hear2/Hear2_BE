package com.hear2.character.controller;

import com.hear2.character.dto.CharacterResponse;
import com.hear2.character.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Character")
@RestController
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "커플 캐릭터 조회")
    @GetMapping("/api/v1/character")
    public CharacterResponse getCharacter(Authentication authentication) {
        return characterService.getCharacter(currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
