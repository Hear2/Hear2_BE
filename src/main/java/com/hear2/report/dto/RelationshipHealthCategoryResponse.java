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
@Schema(description = "관계 건강도 구성 요소")
public class RelationshipHealthCategoryResponse {

    @Schema(description = "카테고리 이름", example = "대화")
    private String name;

    @Schema(description = "도넛 차트 비중", example = "42")
    private int ratio;
}
