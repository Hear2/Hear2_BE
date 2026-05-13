package com.hear2.coupledna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커플 DNA 분석 응답")
public class CoupleDnaResponse {

    @Schema(description = "커플 DNA 타이틀", example = "감정형 탐험가 커플")
    private String dnaTitle;

    @Schema(description = "커플 DNA 설명", example = "ENFP x INFJ 소통 패턴. 최근 120일 동안 감정과 공감이 특히 잘 맞물렸어요.")
    private String dnaDescription;

    @Schema(description = "userA의 대화 성향 타입", example = "ENFP")
    private String userAType;

    @Schema(description = "userB의 대화 성향 타입", example = "INFJ")
    private String userBType;

    @Schema(description = "커플의 핵심 강점 태그", example = "[\"열정\", \"공감\", \"유쾌함\"]")
    private List<String> strengths;

    @Schema(description = "화면용 지표 목록")
    private List<CoupleDnaMetricResponse> metrics;

    @Schema(description = "감성소통 점수", example = "89")
    private int emotionScore;

    @Schema(description = "공감지수 점수", example = "92")
    private int empathyScore;

    @Schema(description = "유머코드 점수", example = "76")
    private int humorScore;

    @Schema(description = "갈등회복 점수", example = "68")
    private int recoveryScore;

    @Schema(description = "계획성 점수", example = "45")
    private int planningScore;

    @Schema(description = "분석에 사용된 데이터 일수", example = "120")
    private int analyzedDays;

    @Schema(description = "DNA 생성 시각", example = "2026-05-13T11:20:00")
    private LocalDateTime generatedAt;

    @Schema(description = "인스타 스토리 공유용 카드 메타데이터")
    private CoupleDnaShareCardResponse shareCard;
}
