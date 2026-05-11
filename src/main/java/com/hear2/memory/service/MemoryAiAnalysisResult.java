package com.hear2.memory.service;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemoryAiAnalysisResult {

    @Builder.Default
    private List<MemoryAiTagCandidate> tags = List.of();

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    private boolean failed = false;

    public static MemoryAiAnalysisResult pending() {
        return MemoryAiAnalysisResult.builder()
                .tags(List.of())
                .completed(false)
                .failed(false)
                .build();
    }

    public static MemoryAiAnalysisResult completed(List<MemoryAiTagCandidate> tags) {
        return MemoryAiAnalysisResult.builder()
                .tags(tags == null ? List.of() : tags)
                .completed(true)
                .failed(false)
                .build();
    }

    public static MemoryAiAnalysisResult failed() {
        return MemoryAiAnalysisResult.builder()
                .tags(List.of())
                .completed(false)
                .failed(true)
                .build();
    }
}
