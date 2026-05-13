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
@Schema(description = "리포트 하단 추억 하이라이트 카드")
public class MemoryHighlightResponse {

    @Schema(description = "카드 상단 라벨", example = "1년 전 오늘")
    private String label;

    @Schema(description = "추억 카드 제목", example = "벚꽃놀이 추억 보러가기")
    private String title;

    @Schema(description = "추억 날짜", example = "2025-05-04")
    private LocalDate date;

    @Schema(description = "추억 장소", example = "서울숲")
    private String location;

    @Schema(description = "추억 썸네일 URL", example = "/api/v1/memories/items/15/photo")
    private String thumbnailUrl;
}
