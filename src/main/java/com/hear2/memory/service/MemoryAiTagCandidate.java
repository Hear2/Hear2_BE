package com.hear2.memory.service;

import java.math.BigDecimal;

public record MemoryAiTagCandidate(
        String tagName,
        BigDecimal confidence
) {
}
