package com.hear2.whatif.dto;

import com.hear2.whatif.support.WhatIfCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfResponse {

    private Long id;
    private String question;
    private WhatIfCategory category;
    private String scenarioSummary;
    private Integer conflictRiskPercent;
    private List<String> riskFactors;
    private String expectedReaction;
    private String advice;
    private List<String> recommendedActions;
    private WhatIfUsedDataSummaryResponse usedDataSummary;
    private String model;
    private LocalDateTime createdAt;
}
