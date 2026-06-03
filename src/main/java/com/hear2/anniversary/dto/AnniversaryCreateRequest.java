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
@Schema(
        description = "사용자 지정 기념일 생성 요청",
        example = """
                {
                  "title": "예진 생일",
                  "type": "BIRTHDAY",
                  "date": "2026-07-14",
                  "ddayType": "D_MINUS",
                  "repeatYearly": true,
                  "shared": true,
                  "icon": "CAKE",
                  "color": "#FFB75E",
                  "notifyDays": [7, 1, 0]
                }
                """
)
public class AnniversaryCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String title;

    @NotNull
    private AnniversaryType type;

    @NotNull
    @Schema(description = "기념일 기준 날짜")
    private LocalDate date;

    @Schema(description = "D-DAY 계산 방식. 기본값은 D_MINUS입니다.")
    private AnniversaryDdayType ddayType = AnniversaryDdayType.D_MINUS;

    @Schema(description = "매년 반복 여부. 생일은 true 권장")
    private boolean repeatYearly;

    @Schema(description = "커플 공유 여부. false면 작성자에게만 표시됩니다.")
    private boolean shared = true;

    @Size(max = 32)
    @Schema(description = "프론트 표시용 아이콘 키", example = "CAKE")
    private String icon;

    @Size(max = 20)
    @Schema(description = "프론트 표시용 색상", example = "#FFB75E")
    private String color;

    @Size(max = 4)
    @Schema(description = "알림 예약 옵션. 7/3/1/0일 전 값만 저장하며 실제 FCM 발송은 후속 구현입니다.")
    private List<Integer> notifyDays;
}
