package com.hear2.global.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "API error response")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error name", example = "Bad Request")
    private String error;

    @Schema(description = "Error message", example = "email: must be a well-formed email address")
    private String message;

    @Schema(description = "Error timestamp", example = "2026-05-07T01:10:00")
    private LocalDateTime timestamp;
}
