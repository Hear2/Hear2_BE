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
@Schema(
        description = "공유 캘린더 일정 생성 요청. userId/coupleId는 JWT 기준 자동 적용됩니다.",
        example = """
                {
                  "title": "서울숲 데이트",
                  "target": "SHARED",
                  "startsAt": "2026-04-12T05:00:00Z",
                  "endsAt": "2026-04-12T10:00:00Z",
                  "allDay": false,
                  "locationName": "서울숲",
                  "addressName": "서울시 성동구 서울숲길 273",
                  "latitude": 37.5444,
                  "longitude": 127.0374,
                  "memo": "벚꽃 보고 카페 들르기",
                  "tags": ["데이트", "봄"],
                  "recurrenceRule": "FREQ=WEEKLY;INTERVAL=1;COUNT=4",
                  "remindBeforeMinutes": 30,
                  "linkedChatMessageId": null,
                  "memoryIds": []
                }
                """
)
public class CalendarEventCreateRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @Schema(description = "MINE=내 개인 일정, PARTNER=상대 개인 일정, SHARED=공동 일정", example = "SHARED")
    private CalendarEventTarget target = CalendarEventTarget.SHARED;

    @NotNull
    @Schema(description = "일정 시작 시각. UTC ISO8601 권장", example = "2026-04-12T05:00:00Z")
    private OffsetDateTime startsAt;

    @Schema(description = "일정 종료 시각. 생략 시 startsAt과 동일하게 저장됩니다.", example = "2026-04-12T10:00:00Z")
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

    @Size(max = 20)
    @Schema(description = "생성 시 함께 연결할 추억 ID 목록", example = "[1, 2]")
    private List<Long> memoryIds;
}
