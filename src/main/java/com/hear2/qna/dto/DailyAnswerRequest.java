package com.hear2.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DailyAnswerRequest(
        @NotBlank
        @Size(max = 500)
        String answer
) {
}
