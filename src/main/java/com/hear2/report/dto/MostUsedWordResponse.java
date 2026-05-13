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
@Schema(description = "워드클라우드 단어 항목")
public class MostUsedWordResponse {

    @Schema(description = "단어", example = "사랑해")
    private String word;

    @Schema(description = "등장 횟수", example = "14")
    private long count;
}
