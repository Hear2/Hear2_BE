package com.hear2.capsule.dto;

import com.hear2.capsule.entity.TimeCapsuleCoverStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(
        description = "타임캡슐 생성 요청. 사진은 /api/v1/media/presigned-url 업로드 후 받은 objectKey를 전달합니다.",
        example = """
                {
                  "name": "1주년 기념 캡슐",
                  "coverStyle": "LETTER",
                  "openAt": "2027-05-16T00:00:00Z",
                  "letter": "지금 이 순간이 너무 행복해서...",
                  "photoObjectKeys": ["media/capsule/7/20260513/photo.jpg"],
                  "options": {
                    "blindOthersAnswer": true,
                    "notifyBeforeOpen": true,
                    "confettiOnOpen": false
                  }
                }
                """
)
public class TimeCapsuleCreateRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "타임캡슐 이름", example = "1주년 기념 캡슐")
    private String name;

    @NotNull
    @Schema(
            description = "커버 스타일. LETTER, GIFT, FLOWER, SPACE, CHERRY 중 하나",
            example = "LETTER"
    )
    private TimeCapsuleCoverStyle coverStyle;

    @NotNull
    @Future
    @Schema(description = "캡슐 개봉 예정 시각. UTC ISO8601 권장", example = "2027-05-16T00:00:00Z")
    private OffsetDateTime openAt;

    @NotBlank
    @Size(max = 5000)
    @Schema(description = "봉인할 편지 내용", example = "지금 이 순간이 너무 행복해 사랑해")
    private String letter;

    @Size(max = 20)
    @Schema(
            description = """
                    캡슐에 넣을 사진 objectKey 목록.
                    /api/v1/media/presigned-url 응답의 objectKey를 넣습니다.
                    URL이나 파일 바이너리를 넣지 않습니다.
                    """,
            example = "[\"media/capsule/17/20260513/9d2c-photo.jpg\"]"
    )
    private List<@Size(max = 2048) String> photoObjectKeys;

    @Schema(description = "타임캡슐 옵션. 알림 관련 옵션은 FCM 연결 후 사용됩니다.")
    private TimeCapsuleOptionsRequest options;
}
