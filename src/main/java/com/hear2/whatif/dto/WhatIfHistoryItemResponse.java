package com.hear2.whatif.dto;

import com.hear2.whatif.support.WhatIfCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfHistoryItemResponse {

    private Long id;
    private String question;
    private WhatIfCategory category;
    private String scenarioSummary;
    private Integer conflictRiskPercent;
    private LocalDateTime createdAt;
}
