package com.hear2.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CharacterNameUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 10)
        String name
) {
}
