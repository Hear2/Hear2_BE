package com.hear2.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "공유 캘린더 일정 수정 요청. 전체 교체 방식입니다.")
public class CalendarEventUpdateRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    private CalendarEventTarget target = CalendarEventTarget.SHARED;

    @NotNull
    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    private boolean allDay;

    @Size(max = 255)
    private String locationName;

    @Size(max = 255)
    private String addressName;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    @Size(max = 1000)
    private String memo;

    @Size(max = 20)
    private List<@Size(max = 80) String> tags;

    @Size(max = 255)
    @Schema(description = "반복 규칙. FREQ=DAILY/WEEKLY/MONTHLY/YEARLY, INTERVAL, COUNT, UNTIL을 지원합니다.", example = "FREQ=WEEKLY;INTERVAL=1;COUNT=4")
    private String recurrenceRule;

    @Min(0)
    @Schema(description = "알림 예정 분 단위. 현재는 저장/응답만 하고 FCM 발송은 후속 작업입니다.", example = "30")
    private Integer remindBeforeMinutes;

    @Schema(description = "이 일정과 관련된 대표 채팅 메시지 ID. 같은 커플 메시지만 연결할 수 있습니다.")
    private Long linkedChatMessageId;
}
