package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "타임캡슐 옵션")
public class TimeCapsuleOptionsRequest {

    @Schema(description = "상대 답변 숨김 옵션. 세부 동작은 프론트/기획 확정 후 적용", example = "true")
    private boolean blindOthersAnswer;

    @Schema(description = "개봉 전 사전 알림 여부. FCM 연결 후 사용", example = "true")
    private boolean notifyBeforeOpen;

    @Schema(description = "개봉 화면 축하 효과 사용 여부", example = "false")
    private boolean confettiOnOpen;
}
