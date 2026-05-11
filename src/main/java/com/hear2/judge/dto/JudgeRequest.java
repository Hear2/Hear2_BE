package com.hear2.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "AI 판사 호출 요청")
public class JudgeRequest {

    @Deprecated
    @Schema(description = "호환용 필드. 로그인 사용자 기준으로 검증되며 생략할 수 있습니다.", example = "1")
    private Long coupleId;

    @Schema(description = "갈등 감지를 유발한 채팅 메시지 ID", example = "100")
    private Long triggerMessageId;

    @Deprecated
    @Schema(description = "호환용 필드. 로그인 사용자 ID로 처리되며 요청 값은 무시됩니다.", example = "10")
    private Long requestedByUserId;
}
