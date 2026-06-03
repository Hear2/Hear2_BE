package com.hear2.couple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(
        description = "커플 사귄 날 설정 요청",
        example = """
                {
                  "startDate": "2024-12-20"
                }
                """
)
public class CoupleStartDateRequest {

    @NotNull
    @Schema(description = "사귄 날. D+와 자동 기념일 계산 기준입니다.", example = "2024-12-20")
    private LocalDate startDate;
}
