package com.hear2.whatif.service;

import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.whatif.client.WhatIfLlmClient;
import com.hear2.whatif.dto.WhatIfHistoryResponse;
import com.hear2.whatif.dto.WhatIfResponse;
import com.hear2.whatif.dto.WhatIfSimulateRequest;
import com.hear2.whatif.dto.WhatIfUsedDataSummaryResponse;
import com.hear2.whatif.entity.WhatIfHistory;
import com.hear2.whatif.repository.WhatIfHistoryRepository;
import com.hear2.whatif.support.WhatIfCategory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatIfServiceTest {

    private final WhatIfHistoryRepository whatIfHistoryRepository = mock(WhatIfHistoryRepository.class);
    private final WhatIfContextCollector contextCollector = mock(WhatIfContextCollector.class);
    private final WhatIfPromptBuilder promptBuilder = mock(WhatIfPromptBuilder.class);
    private final WhatIfLlmClient whatIfLlmClient = mock(WhatIfLlmClient.class);
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WhatIfAiResponseParser responseParser = new WhatIfAiResponseParser(objectMapper);
    private final WhatIfService service = new WhatIfService(
            whatIfHistoryRepository,
            contextCollector,
            promptBuilder,
            whatIfLlmClient,
            responseParser,
            chatParticipantResolver,
            objectMapper
    );

    @Test
    void rejectsBlankQuestion() {
        WhatIfSimulateRequest request = new WhatIfSimulateRequest();
        request.setQuestion(" ");

        assertThatThrownBy(() -> service.simulate(10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("question must not be blank");
    }

    @Test
    void rejectsQuestionLongerThan300Characters() {
        WhatIfSimulateRequest request = new WhatIfSimulateRequest();
        request.setQuestion("a".repeat(301));

        assertThatThrownBy(() -> service.simulate(10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("question must be 300 characters or less");
    }

    @Test
    void savesUsingLoggedInUsersCoupleId() {
        WhatIfSimulateRequest request = new WhatIfSimulateRequest();
        request.setQuestion("주말 계획 없이 만나면?");
        request.setCategory(WhatIfCategory.DATE);
        WhatIfContext context = context();

        when(contextCollector.collect(10L)).thenReturn(context);
        when(promptBuilder.build(request.getQuestion(), WhatIfCategory.DATE, context)).thenReturn("prompt");
        when(whatIfLlmClient.generateJson("prompt")).thenReturn(Optional.of("""
                {
                  "scenarioSummary": "summary",
                  "conflictRiskPercent": 42,
                  "riskFactors": ["risk"],
                  "expectedReaction": "reaction",
                  "advice": "advice",
                  "recommendedActions": ["action"]
                }
                """));
        when(whatIfLlmClient.model()).thenReturn("gpt-4o-mini");
        when(whatIfHistoryRepository.save(any(WhatIfHistory.class)))
                .thenAnswer(invocation -> copyWithId(invocation.getArgument(0), 1L));

        WhatIfResponse response = service.simulate(10L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategory()).isEqualTo(WhatIfCategory.DATE);
        assertThat(response.getConflictRiskPercent()).isEqualTo(42);

        ArgumentCaptor<WhatIfHistory> captor = ArgumentCaptor.forClass(WhatIfHistory.class);
        verify(whatIfHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getCoupleId()).isEqualTo(1L);
        assertThat(captor.getValue().getRequesterId()).isEqualTo(10L);
        assertThat(captor.getValue().getQuestion()).isEqualTo("주말 계획 없이 만나면?");
    }

    @Test
    void detailUsesIdAndLoggedInUsersCoupleId() throws Exception {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 20L));
        WhatIfHistory history = history(3L, 1L, LocalDateTime.of(2026, 5, 15, 12, 0));
        when(whatIfHistoryRepository.findByIdAndCoupleId(3L, 1L)).thenReturn(Optional.of(history));

        WhatIfResponse response = service.getDetail(10L, 3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getScenarioSummary()).isEqualTo("summary");
        verify(whatIfHistoryRepository).findByIdAndCoupleId(3L, 1L);
    }

    @Test
    void rejectsDetailFromAnotherCouple() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 20L));
        when(whatIfHistoryRepository.findByIdAndCoupleId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(10L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("what-if history not found");
    }

    @Test
    void historyReturnsRepositoryOrder() throws Exception {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 20L));
        when(whatIfHistoryRepository.findTop30ByCoupleIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(
                        history(2L, 1L, LocalDateTime.of(2026, 5, 15, 13, 0)),
                        history(1L, 1L, LocalDateTime.of(2026, 5, 15, 12, 0))
                ));

        WhatIfHistoryResponse response = service.getHistory(10L);

        assertThat(response.getItems()).extracting("id").containsExactly(2L, 1L);
        verify(whatIfHistoryRepository).findTop30ByCoupleIdOrderByCreatedAtDesc(1L);
    }

    private WhatIfContext context() {
        WhatIfUsedDataSummaryResponse summary = WhatIfUsedDataSummaryResponse.builder()
                .coupleDnaUsed(true)
                .emotionDays(30)
                .chatMessageCount(12)
                .limitedRecentMessageCount(3)
                .judgeHistoryCount(1)
                .qnaAnswerCount(2)
                .memoryCount(4)
                .build();
        return new WhatIfContext(1L, 10L, new LinkedHashMap<>(), summary);
    }

    private WhatIfHistory copyWithId(WhatIfHistory history, Long id) {
        return WhatIfHistory.builder()
                .id(id)
                .coupleId(history.getCoupleId())
                .requesterId(history.getRequesterId())
                .question(history.getQuestion())
                .category(history.getCategory())
                .aiResponseJson(history.getAiResponseJson())
                .usedDataSummaryJson(history.getUsedDataSummaryJson())
                .model(history.getModel())
                .createdAt(LocalDateTime.of(2026, 5, 15, 12, 0))
                .build();
    }

    private WhatIfHistory history(Long id, Long coupleId, LocalDateTime createdAt) throws Exception {
        WhatIfAiContent aiContent = WhatIfAiContent.builder()
                .scenarioSummary("summary")
                .conflictRiskPercent(42)
                .riskFactors(List.of("risk"))
                .expectedReaction("reaction")
                .advice("advice")
                .recommendedActions(List.of("action"))
                .build();
        return WhatIfHistory.builder()
                .id(id)
                .coupleId(coupleId)
                .requesterId(10L)
                .question("question")
                .category(WhatIfCategory.GENERAL)
                .aiResponseJson(objectMapper.writeValueAsString(aiContent))
                .usedDataSummaryJson(objectMapper.writeValueAsString(WhatIfUsedDataSummaryResponse.builder().build()))
                .model("gpt-4o-mini")
                .createdAt(createdAt)
                .build();
    }
}
