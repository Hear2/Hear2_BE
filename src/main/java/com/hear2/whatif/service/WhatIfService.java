package com.hear2.whatif.service;

import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.whatif.client.WhatIfLlmClient;
import com.hear2.whatif.dto.WhatIfHistoryItemResponse;
import com.hear2.whatif.dto.WhatIfHistoryResponse;
import com.hear2.whatif.dto.WhatIfResponse;
import com.hear2.whatif.dto.WhatIfSimulateRequest;
import com.hear2.whatif.dto.WhatIfUsedDataSummaryResponse;
import com.hear2.whatif.entity.WhatIfHistory;
import com.hear2.whatif.repository.WhatIfHistoryRepository;
import com.hear2.whatif.support.WhatIfCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatIfService {

    private static final int QUESTION_MAX_LENGTH = 300;

    private final WhatIfHistoryRepository whatIfHistoryRepository;
    private final WhatIfContextCollector contextCollector;
    private final WhatIfPromptBuilder promptBuilder;
    private final WhatIfLlmClient whatIfLlmClient;
    private final WhatIfAiResponseParser responseParser;
    private final ChatParticipantResolver chatParticipantResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public WhatIfResponse simulate(Long currentUserId, WhatIfSimulateRequest request) {
        String question = validateAndNormalizeQuestion(request == null ? null : request.getQuestion());
        WhatIfCategory category = request == null || request.getCategory() == null
                ? WhatIfCategory.GENERAL
                : request.getCategory();

        WhatIfContext context = contextCollector.collect(currentUserId);
        String prompt = promptBuilder.build(question, category, context);
        WhatIfAiContent aiContent = whatIfLlmClient.generateJson(prompt)
                .map(responseParser::parseOrFallback)
                .orElseGet(responseParser::fallback);

        WhatIfHistory savedHistory = whatIfHistoryRepository.save(WhatIfHistory.builder()
                .coupleId(context.coupleId())
                .requesterId(context.requesterId())
                .question(question)
                .category(category)
                .aiResponseJson(writeJson(aiContent, "AI response"))
                .usedDataSummaryJson(writeJson(context.usedDataSummary(), "used data summary"))
                .model(whatIfLlmClient.model())
                .build());

        return toResponse(savedHistory, aiContent, context.usedDataSummary());
    }

    @Transactional(readOnly = true)
    public WhatIfHistoryResponse getHistory(Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        List<WhatIfHistoryItemResponse> items = whatIfHistoryRepository
                .findTop30ByCoupleIdOrderByCreatedAtDesc(coupleId)
                .stream()
                .map(this::toHistoryItem)
                .toList();
        return new WhatIfHistoryResponse(items);
    }

    @Transactional(readOnly = true)
    public WhatIfResponse getDetail(Long currentUserId, Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required");
        }

        Long coupleId = resolveCoupleId(currentUserId);
        WhatIfHistory history = whatIfHistoryRepository.findByIdAndCoupleId(id, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "what-if history not found"));
        return toResponse(history, readAiContent(history), readUsedDataSummary(history));
    }

    private Long resolveCoupleId(Long currentUserId) {
        return chatParticipantResolver.resolve(currentUserId).coupleId();
    }

    private WhatIfHistoryItemResponse toHistoryItem(WhatIfHistory history) {
        WhatIfAiContent aiContent = readAiContent(history);
        return WhatIfHistoryItemResponse.builder()
                .id(history.getId())
                .question(history.getQuestion())
                .category(history.getCategory())
                .scenarioSummary(aiContent.getScenarioSummary())
                .conflictRiskPercent(aiContent.getConflictRiskPercent())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private WhatIfResponse toResponse(
            WhatIfHistory history,
            WhatIfAiContent aiContent,
            WhatIfUsedDataSummaryResponse usedDataSummary
    ) {
        return WhatIfResponse.builder()
                .id(history.getId())
                .question(history.getQuestion())
                .category(history.getCategory())
                .scenarioSummary(aiContent.getScenarioSummary())
                .conflictRiskPercent(aiContent.getConflictRiskPercent())
                .riskFactors(aiContent.getRiskFactors())
                .expectedReaction(aiContent.getExpectedReaction())
                .advice(aiContent.getAdvice())
                .recommendedActions(aiContent.getRecommendedActions())
                .usedDataSummary(usedDataSummary)
                .model(history.getModel())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private WhatIfAiContent readAiContent(WhatIfHistory history) {
        try {
            return responseParser.normalize(objectMapper.readValue(history.getAiResponseJson(), WhatIfAiContent.class));
        } catch (Exception ignored) {
            return responseParser.fallback();
        }
    }

    private WhatIfUsedDataSummaryResponse readUsedDataSummary(WhatIfHistory history) {
        try {
            return objectMapper.readValue(history.getUsedDataSummaryJson(), WhatIfUsedDataSummaryResponse.class);
        } catch (Exception ignored) {
            return WhatIfUsedDataSummaryResponse.builder().build();
        }
    }

    private String writeJson(Object value, String label) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize " + label, ex);
        }
    }

    private String validateAndNormalizeQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank");
        }

        String normalized = question.trim();
        if (normalized.length() > QUESTION_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must be 300 characters or less");
        }
        return normalized;
    }
}
