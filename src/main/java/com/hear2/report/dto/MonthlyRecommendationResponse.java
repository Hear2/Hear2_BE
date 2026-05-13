package com.hear2.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "월간 AI 추천 카드")
public class MonthlyRecommendationResponse {

    @Schema(description = "프론트 아이콘 타입", example = "HEART")
    private String iconType;

    @Schema(description = "추천 제목", example = "주말 데이트 빈도 +1")
    private String title;

    @Schema(description = "추천 설명", example = "이번 달엔 토요일 한 번 더 같이 시간을 보내보세요.")
    private String description;
}
