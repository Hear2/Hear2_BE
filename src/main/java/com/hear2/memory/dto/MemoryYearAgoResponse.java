package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "1년 전 오늘 추억 응답")
public class MemoryYearAgoResponse {

    private boolean exists;
    private List<MemoryResponse> items;
    private String summary;
}
