package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "이미지 AI 태그 수동 생성 응답")
public class MemoryImageTagResponse {

    private List<String> tags;
    private String scene;
    private BigDecimal confidence;
}
