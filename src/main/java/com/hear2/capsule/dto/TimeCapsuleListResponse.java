package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TimeCapsuleListResponse {

    @Schema(description = "아직 열리지 않은 캡슐 목록")
    private List<TimeCapsuleSummaryResponse> sealed;

    @Schema(description = "열린 캡슐 목록")
    private List<TimeCapsuleSummaryResponse> open;
}
