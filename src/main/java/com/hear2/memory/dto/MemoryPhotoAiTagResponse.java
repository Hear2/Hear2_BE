package com.hear2.memory.dto;

import com.hear2.memory.entity.MemoryPhotoAiTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "사진별 AI 태그")
public class MemoryPhotoAiTagResponse {

    @Schema(description = "사진별 태그 ID", example = "31")
    private Long id;

    @Schema(description = "태그명. # 없이 저장됩니다.", example = "축구")
    private String tagName;

    @Schema(description = "AI 신뢰도", example = "0.8400")
    private BigDecimal confidence;

    public static MemoryPhotoAiTagResponse from(MemoryPhotoAiTag tag) {
        return MemoryPhotoAiTagResponse.builder()
                .id(tag.getId())
                .tagName(tag.getTagName())
                .confidence(tag.getConfidence())
                .build();
    }
}
