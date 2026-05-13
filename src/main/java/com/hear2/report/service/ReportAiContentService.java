package com.hear2.report.service;

import com.hear2.report.dto.MonthlyRecommendationResponse;
import com.hear2.report.support.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAiContentService {

    private static final List<String> ALLOWED_ICON_TYPES = List.of("HEART", "CAMERA", "CHAT", "SPARKLES", "FLOWER", "MAP");
    private static final int MAX_RECOMMENDATIONS = 3;

    private final ReportLlmClient reportLlmClient;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/report-content-v1.txt")
    private Resource promptResource;

    public Optional<ReportAiContentResult> generate(ReportType reportType, Map<String, Object> reportData) {
        if (reportType == null || reportData == null || reportData.isEmpty() || !reportLlmClient.isAvailable()) {
            return Optional.empty();
        }

        try {
            String prompt = buildPrompt(reportType, reportData);
            return reportLlmClient.generateJson(prompt)
                    .flatMap(this::parseResponse);
        } catch (Exception ex) {
            log.warn("Report AI content generation fallback triggered: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(ReportType reportType, Map<String, Object> reportData) throws IOException {
        StringBuilder prompt = new StringBuilder(loadPromptTemplate());
        prompt.append("\n\nreportType: ").append(reportType.name());
        prompt.append("\nresponseRules:");
        prompt.append("\n- summary는 한국어 1~2문장");
        prompt.append("\n- 톤은 다정하고 가볍게, 과장하지 말기");
        prompt.append("\n- 민감하거나 부정적인 표현은 세게 쓰지 말기");
        prompt.append("\n- 추천은 실행 가능한 행동 중심");
        prompt.append("\n- monthly 추천은 최대 3개");
        prompt.append("\n- iconType allowed: ").append(String.join(", ", ALLOWED_ICON_TYPES));
        prompt.append("\n- 반드시 JSON 객체만 응답하기");

        if (reportType == ReportType.WEEKLY) {
            prompt.append("\n- weekly는 summary만 생성하고 recommendations는 빈 배열 또는 생략");
        } else {
            prompt.append("\n- monthly는 summary와 recommendations를 함께 생성");
        }

        prompt.append("\n- oneAnswerResponseCount가 null이면 미연동 상태로 이해");
        prompt.append("\n\naggregatedReportData:\n");
        prompt.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportData));

        return prompt.toString();
    }

    private Optional<ReportAiContentResult> parseResponse(String outputText) {
        if (!StringUtils.hasText(outputText)) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(outputText));
            String summary = root.path("summary").asString("").trim();
            List<MonthlyRecommendationResponse> recommendations = parseRecommendations(root.path("recommendations"));

            if (!StringUtils.hasText(summary) && recommendations.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ReportAiContentResult(
                    StringUtils.hasText(summary) ? summary : null,
                    recommendations
            ));
        } catch (Exception ex) {
            log.warn("Report AI content parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private List<MonthlyRecommendationResponse> parseRecommendations(JsonNode recommendationsNode) {
        if (recommendationsNode == null || !recommendationsNode.isArray()) {
            return List.of();
        }

        List<MonthlyRecommendationResponse> recommendations = new ArrayList<>();
        for (JsonNode node : recommendationsNode) {
            MonthlyRecommendationResponse recommendation = MonthlyRecommendationResponse.builder()
                    .iconType(normalizeIconType(node.path("iconType").asText(null)))
                    .title(trimToNull(node.path("title").asText(null)))
                    .description(trimToNull(node.path("description").asText(null)))
                    .build();

            if (!StringUtils.hasText(recommendation.getTitle())
                    || !StringUtils.hasText(recommendation.getDescription())) {
                continue;
            }

            recommendations.add(recommendation);
            if (recommendations.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        return recommendations;
    }

    private String normalizeIconType(String iconType) {
        if (!StringUtils.hasText(iconType)) {
            return "HEART";
        }

        String normalized = iconType.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ICON_TYPES.contains(normalized) ? normalized : "HEART";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private String loadPromptTemplate() throws IOException {
        return StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
    }

    public record ReportAiContentResult(
            String summary,
            List<MonthlyRecommendationResponse> recommendations
    ) {
    }
}
