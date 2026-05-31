package com.hear2.whatif.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfUsedDataSummaryResponse {

    private boolean coupleDnaUsed;
    private int emotionDays;
    private long chatMessageCount;
    private int limitedRecentMessageCount;
    private int judgeHistoryCount;
    private int qnaAnswerCount;
    private int memoryCount;
}
