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
@Schema(description = "커플 DNA 공유 카드 하단 사용자 카드 메타데이터")
public class CoupleDnaShareUserCardResponse {

    @Schema(description = "프론트 구분용 슬롯", example = "USER_A")
    private String slot;

    @Schema(description = "사용자 성향 타입", example = "ENFP")
    private String type;

    @Schema(description = "사용자 카드 강조 색상", example = "#FF5F96")
    private String accentColor;

    @Schema(description = "사용자 카드 태그 목록", example = "[\"열정\", \"직관\", \"공감\"]")
    private List<String> traits;
}
