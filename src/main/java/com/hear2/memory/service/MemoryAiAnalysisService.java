package com.hear2.memory.service;

import com.hear2.memory.dto.MemoryCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MemoryAiAnalysisService {

    private static final String RESPONSES_PATH = "/responses";
    private static final int MAX_TAGS = 5;
    private static final BigDecimal MIN_CONFIDENCE = BigDecimal.ZERO;
    private static final BigDecimal MAX_CONFIDENCE = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.7000");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String promptTemplate;

    public MemoryAiAnalysisService(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("classpath:prompts/memory-photo-tags-v1.txt") Resource promptResource
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.promptTemplate = loadPrompt(promptResource);
    }

    public MemoryAiAnalysisResult analyze(MemoryPhotoFile photo, MemoryCreateRequest request) {
        if (!StringUtils.hasText(apiKey)) {
            return MemoryAiAnalysisResult.pending();
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(
                            toDataUrl(photo),
                            request == null ? null : request.getMemo(),
                            request == null ? null : request.getLocationName(),
                            request == null ? null : request.getTakenAt()
                    ))
                    .retrieve()
                    .body(Map.class);

            List<MemoryAiTagCandidate> tags = parseTags(extractOutputText(response));
            return MemoryAiAnalysisResult.completed(tags);
        } catch (IOException | RuntimeException e) {
            log.warn("Memory AI analysis failed: {}", e.getMessage());
            return MemoryAiAnalysisResult.failed();
        }
    }

    public MemoryAiAnalysisResult analyzeImageUrl(
            String imageUrl,
            String memo,
            String locationName,
            LocalDateTime takenAt
    ) {
        if (!StringUtils.hasText(apiKey)) {
            return MemoryAiAnalysisResult.pending();
        }
        if (!StringUtils.hasText(imageUrl)) {
            return MemoryAiAnalysisResult.pending();
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(RESPONSES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(imageUrl.trim(), memo, locationName, takenAt))
                    .retrieve()
                    .body(Map.class);

            List<MemoryAiTagCandidate> tags = parseTags(extractOutputText(response));
            return MemoryAiAnalysisResult.completed(tags);
        } catch (IOException | RuntimeException e) {
            log.warn("Memory AI analysis failed: {}", e.getMessage());
            return MemoryAiAnalysisResult.failed();
        }
    }

    private Map<String, Object> buildRequestBody(
            String imageUrl,
            String memo,
            String locationName,
            LocalDateTime takenAt
    ) throws IOException {
        return Map.of(
                "model", model,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", buildPrompt(memo, locationName, takenAt)
                                ),
                                Map.of(
                                        "type", "input_image",
                                        "image_url", imageUrl,
                                        "detail", "low"
                                )
                        )
                )),
                "max_output_tokens", 350
        );
    }

    private String buildPrompt(String memo, String locationName, LocalDateTime takenAt) {
        StringBuilder prompt = new StringBuilder(promptTemplate);

        prompt.append("\n\nContext provided by user:");
        if (StringUtils.hasText(memo)) {
            prompt.append("\n- memo: ").append(memo.trim());
        }
        if (StringUtils.hasText(locationName)) {
            prompt.append("\n- locationName: ").append(locationName.trim());
        }
        if (takenAt != null) {
            prompt.append("\n- takenAt: ").append(takenAt);
        }

        return prompt.toString();
    }

    private String toDataUrl(MemoryPhotoFile photo) {
        return "data:"
                + photo.contentType()
                + ";base64,"
                + Base64.getEncoder().encodeToString(photo.content());
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

    private List<MemoryAiTagCandidate> parseTags(String outputText) throws IOException {
        if (!StringUtils.hasText(outputText)) {
            return List.of();
        }

        JsonNode root = objectMapper.readTree(stripCodeFence(outputText));
        JsonNode tagsNode = root.path("tags");
        if (!tagsNode.isArray()) {
            return List.of();
        }

        List<MemoryAiTagCandidate> tags = new ArrayList<>();
        for (JsonNode tagNode : tagsNode) {
            String tagName = tagNode.path("tagName").asString("").trim();
            if (!StringUtils.hasText(tagName)) {
                continue;
            }

            tags.add(new MemoryAiTagCandidate(
                    normalizeTagName(tagName),
                    resolveConfidence(tagNode.path("confidence"))
            ));
            if (tags.size() >= MAX_TAGS) {
                break;
            }
        }

        return tags;
    }

    private String stripCodeFence(String outputText) {
        String trimmed = outputText.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineEnd = trimmed.indexOf('\n');
        int lastFenceStart = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFenceStart <= firstLineEnd) {
            return trimmed;
        }

        return trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
    }

    private String normalizeTagName(String tagName) {
        return tagName.replace("#", "").trim();
    }

    private BigDecimal resolveConfidence(JsonNode confidenceNode) {
        if (confidenceNode == null || confidenceNode.isMissingNode() || !confidenceNode.isNumber()) {
            return DEFAULT_CONFIDENCE;
        }

        BigDecimal confidence = confidenceNode.decimalValue();
        if (confidence.compareTo(MIN_CONFIDENCE) < 0) {
            return MIN_CONFIDENCE.setScale(4, RoundingMode.HALF_UP);
        }
        if (confidence.compareTo(MAX_CONFIDENCE) > 0) {
            return MAX_CONFIDENCE.setScale(4, RoundingMode.HALF_UP);
        }

        return confidence.setScale(4, RoundingMode.HALF_UP);
    }

    private String loadPrompt(Resource promptResource) {
        try {
            return StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load memory photo tag prompt", e);
        }
    }
}
