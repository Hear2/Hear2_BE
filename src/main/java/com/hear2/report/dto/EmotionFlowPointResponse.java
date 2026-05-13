package com.hear2.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기간 내 감정 흐름 집계 포인트")
public class EmotionFlowPointResponse {

    @Schema(description = "표시 라벨", example = "월")
    private String label;

    @Schema(description = "집계 날짜", example = "2026-05-04")
    private LocalDate date;

    @Schema(description = "주간 리포트용 요일", example = "MONDAY")
    private String dayOfWeek;

    @Schema(description = "월간 리포트용 주차 라벨", example = "1주")
    private String weekLabel;

    @Schema(description = "월간 리포트용 구간 시작일", example = "2026-05-01")
    private LocalDate startDate;

    @Schema(description = "월간 리포트용 구간 종료일", example = "2026-05-07")
    private LocalDate endDate;

    @Schema(description = "해당 날짜의 우세 감정")
    private EmotionSummaryResponse dominantEmotion;

    @Schema(description = "해당 날짜의 긍정 비율(감정 분석된 TEXT 메시지 기준)", example = "100")
    private int positiveRatio;

    @Schema(description = "해당 날짜의 부정 비율(감정 분석된 TEXT 메시지 기준)", example = "0")
    private int negativeRatio;

    @Schema(description = "해당 날짜의 감정 분석된 TEXT 메시지 수", example = "2")
    private long messageCount;

    @Schema(description = "프론트 그래프 렌더링용 점수. 현재 positiveRatio 기반", example = "100")
    private int score;
}
