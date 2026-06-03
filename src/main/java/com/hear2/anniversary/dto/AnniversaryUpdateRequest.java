package com.hear2.anniversary.dto;

import com.hear2.anniversary.support.AnniversaryDdayType;
import com.hear2.anniversary.support.AnniversaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "사용자 지정 기념일 수정 요청")
public class AnniversaryUpdateRequest {

    @NotBlank
    @Size(max = 50)
    private String title;

    @NotNull
    private AnniversaryType type;

    @NotNull
    private LocalDate date;

    private AnniversaryDdayType ddayType = AnniversaryDdayType.D_MINUS;

    private boolean repeatYearly;

    private boolean shared = true;

    @Size(max = 32)
    private String icon;

    @Size(max = 20)
    private String color;

    @Size(max = 4)
    private List<Integer> notifyDays;
}
