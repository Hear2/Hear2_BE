package com.hear2.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OpenAiReportLlmClient implements ReportLlmClient {

    private static final String RESPONSES_PATH = "/responses";

    private final boolean enabled;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiReportLlmClient(
            @Value("${openai.report.enabled:true}") boolean enabled,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.report.model:${openai.model:gpt-4o-mini}}") String model,
            @Value("${openai.report.connect-timeout-ms:1500}") int connectTimeoutMs,
            @Value("${openai.report.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public boolean isAvailable() {
        return enabled && StringUtils.hasText(apiKey);
    }

    @Override
    public Optional<String> generateJson(String prompt) {
        if (!isAvailable() || !StringUtils.hasText(prompt)) {
            return Optional.empty();
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt))
                    .retrieve()
                    .body(Map.class);

            String outputText = extractOutputText(response);
            return StringUtils.hasText(outputText) ? Optional.of(outputText) : Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Report LLM generation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", model,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", prompt
                        ))
                )),
                "max_output_tokens", 900
        );
    }

    private String extractOutputText(Map<?, ?> response) {
        if (response == null) {
            return "";
        }

        Object outputText = response.get("output_text");
        if (outputText instanceof String text) {
            return text;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }

            Object content = outputMap.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }

            for (Object contentItem : contentItems) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }

                Object contentText = contentMap.get("text");
                if (contentText instanceof String value) {
                    text.append(value);
                }
            }
        }

        return text.toString();
    }
}
