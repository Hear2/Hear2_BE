package com.hear2.judge.dto;

import com.hear2.judge.enums.ConflictType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "반복 갈등 패턴 응답")
public class ConflictPatternResponse {

    @Schema(description = "갈등 유형", example = "COMMUNICATION")
    private ConflictType conflictType;

    @Schema(description = "동일 갈등 유형 누적 횟수", example = "3")
    private Long count;

    @Schema(description = "마지막 판결 생성 시각", example = "2026-05-09T16:30:00")
    private LocalDateTime latestCreatedAt;
}
