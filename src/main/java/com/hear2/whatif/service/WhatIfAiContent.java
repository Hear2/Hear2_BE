package com.hear2.whatif.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfAiContent {

    private String scenarioSummary;
    private Integer conflictRiskPercent;
    private List<String> riskFactors;
    private String expectedReaction;
    private String advice;
    private List<String> recommendedActions;
}
