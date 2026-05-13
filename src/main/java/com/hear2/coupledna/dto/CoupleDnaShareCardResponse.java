package com.hear2.coupledna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인스타 스토리 공유용 커플 DNA 카드 메타데이터")
public class CoupleDnaShareCardResponse {

    @Schema(description = "분석 완료 배지 문구", example = "120일 데이터 분석 완료")
    private String badgeText;

    @Schema(description = "카드 메인 타이틀", example = "감정형 탐험가 커플")
    private String headline;

    @Schema(description = "카드 서브 타이틀", example = "ENFP x INFJ 소통 패턴")
    private String subheadline;

    @Schema(description = "배경 그라데이션 시작 색상", example = "#FF4F93")
    private String gradientStartColor;

    @Schema(description = "배경 그라데이션 종료 색상", example = "#B69CFF")
    private String gradientEndColor;

    @Schema(description = "카드 상단 핵심 배지 목록")
    private List<CoupleDnaShareMetricBadgeResponse> metricBadges;

    @Schema(description = "하단 사용자 카드 목록")
    private List<CoupleDnaShareUserCardResponse> userCards;

    @Schema(description = "공유 카드용 보조 문구", example = "우리의 공감과 감정 리듬이 특히 또렷했던 기간이에요.")
    private String footerMessage;
}
