package com.hear2.anniversary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(
        description = "커플 시작일 설정 요청",
        example = """
                {
                  "startDate": "2024-12-20"
                }
                """
)
public class AnniversaryStartDateRequest {

    @NotNull
    @Schema(description = "사귄 날짜. D+ 계산 기준일입니다.", example = "2024-12-20")
    private LocalDate startDate;
}
