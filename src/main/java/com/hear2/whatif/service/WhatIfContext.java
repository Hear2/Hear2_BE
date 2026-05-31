package com.hear2.whatif.service;

import com.hear2.whatif.dto.WhatIfUsedDataSummaryResponse;

import java.util.Map;

public record WhatIfContext(
        Long coupleId,
        Long requesterId,
        Map<String, Object> promptData,
        WhatIfUsedDataSummaryResponse usedDataSummary
) {
}
