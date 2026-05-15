package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "인스타 스토리 공유용 타임캡슐 카드 메타데이터")
public class TimeCapsuleShareCardResponse {

    @Schema(description = "카드 상단 배지 문구", example = "TIME CAPSULE OPEN")
    private String badgeText;

    @Schema(description = "카드 메인 타이틀", example = "1주년 기념 캡슐이 열렸어요")
    private String headline;

    @Schema(description = "카드 서브 타이틀", example = "1년 전 오늘, 우리가 봉인한 추억")
    private String subheadline;

    @Schema(description = "봉인/개봉 날짜 문구", example = "봉인: 2026.05.13 -> 오픈: 2027.05.13")
    private String dateText;

    @Schema(description = "공유 카드에 표시할 사진 개수", example = "3")
    private int photoCount;

    @Schema(description = "배경 그라데이션 시작 색상", example = "#6D5BFF")
    private String gradientStartColor;

    @Schema(description = "배경 그라데이션 종료 색상", example = "#FF4F93")
    private String gradientEndColor;

    @Schema(description = "카드 장식/강조 문구", example = "우리가 남겨둔 마음이 도착했어요")
    private String highlightText;

    @Schema(description = "공유 카드용 하단 문구", example = "Hear2 Time Capsule")
    private String footerMessage;
}
