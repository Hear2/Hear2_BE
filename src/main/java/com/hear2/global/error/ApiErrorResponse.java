package com.hear2.global.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "API 오류 응답")
public class ApiErrorResponse {

    @Schema(description = "HTTP 상태 코드", example = "400")
    private int status;

    @Schema(description = "HTTP 오류 이름", example = "Bad Request")
    private String error;

    @Schema(description = "오류 메시지", example = "content is required for emotion analysis")
    private String message;

    @Schema(description = "오류 발생 시각", example = "2026-05-07T01:10:00")
    private LocalDateTime timestamp;
}
