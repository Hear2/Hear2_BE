package com.hear2.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "AI 판사 호출 요청")
public class JudgeRequest {

    @Schema(description = "커플 ID", example = "1")
    private Long coupleId;

    @Schema(description = "갈등 감지를 유발한 채팅 메시지 ID", example = "100")
    private Long triggerMessageId;

    @Schema(description = "AI 판사 버튼을 누른 사용자 ID. 선택 값입니다.", example = "10")
    private Long requestedByUserId;
}
