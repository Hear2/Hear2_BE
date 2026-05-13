package com.hear2.report.dto;

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
@Schema(description = "월간 관계 건강도")
public class RelationshipHealthResponse {

    @Schema(description = "건강도 점수", example = "87")
    private int score;

    @Schema(description = "카테고리별 비중")
    private List<RelationshipHealthCategoryResponse> categories;
}
