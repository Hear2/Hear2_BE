package com.hear2.memory.dto;

import com.hear2.memory.entity.MemoryAiTag;
import com.hear2.memory.entity.MemoryTagSource;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MemoryAiTagResponse {

    private Long id;
    private String tagName;
    private BigDecimal confidence;
    private MemoryTagSource source;

    public static MemoryAiTagResponse from(MemoryAiTag tag) {
        return MemoryAiTagResponse.builder()
                .id(tag.getId())
                .tagName(tag.getTagName())
                .confidence(tag.getConfidence())
                .source(tag.getSource())
                .build();
    }
}
