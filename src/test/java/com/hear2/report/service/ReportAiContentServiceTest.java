package com.hear2.report.service;

import com.hear2.report.support.ReportType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAiContentServiceTest {

    @Test
    void parsesWeeklySummaryFromLlmJsonResponse() {
        ReportAiContentService service = new ReportAiContentService(
                new StubReportLlmClient(true, Optional.of("""
                        {
                          "summary": "이번 주는 서로 대화가 꽤 다정하게 이어진 한 주였어요 💕"
                        }
                        """)),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "promptResource", new ByteArrayResource(
                "Return JSON only".getBytes(StandardCharsets.UTF_8)
        ));

        Optional<ReportAiContentService.ReportAiContentResult> result = service.generate(
                ReportType.WEEKLY,
                Map.of(
                        "periodStart", "2026-05-04",
                        "periodEnd", "2026-05-10",
                        "totalConversationCount", 120
                )
        );

        assertThat(result).isPresent();
        assertThat(result.get().summary()).contains("다정하게");
        assertThat(result.get().recommendations()).isEmpty();
    }

    @Test
    void parsesMonthlyRecommendationsAndNormalizesIconType() {
        ReportAiContentService service = new ReportAiContentService(
                new StubReportLlmClient(true, Optional.of("""
                        ```json
                        {
                          "summary": "4월은 조금 더 편안해진 달이었어요 🌷",
                          "recommendations": [
                            {
                              "iconType": "flower",
                              "title": "봄 산책 코스 도장깨기",
                              "description": "서울숲이나 한강처럼 편하게 걸을 수 있는 곳을 같이 걸어보세요."
                            }
                          ]
                        }
                        ```
                        """)),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "promptResource", new ByteArrayResource(
                "Return JSON only".getBytes(StandardCharsets.UTF_8)
        ));

        Optional<ReportAiContentService.ReportAiContentResult> result = service.generate(
                ReportType.MONTHLY,
                Map.of(
                        "periodStart", "2026-04-01",
                        "periodEnd", "2026-04-30",
                        "mostUsedWords", List.of(Map.of("word", "사랑해", "count", 12))
                )
        );

        assertThat(result).isPresent();
        assertThat(result.get().summary()).contains("4월");
        assertThat(result.get().recommendations()).hasSize(1);
        assertThat(result.get().recommendations().get(0).getIconType()).isEqualTo("FLOWER");
    }

    @Test
    void returnsEmptyWhenLlmResponseIsInvalidJson() {
        ReportAiContentService service = new ReportAiContentService(
                new StubReportLlmClient(true, Optional.of("요약만 보낼게요")),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "promptResource", new ByteArrayResource(
                "Return JSON only".getBytes(StandardCharsets.UTF_8)
        ));

        Optional<ReportAiContentService.ReportAiContentResult> result = service.generate(
                ReportType.WEEKLY,
                Map.of("periodStart", "2026-05-04")
        );

        assertThat(result).isEmpty();
    }

    private record StubReportLlmClient(boolean available, Optional<String> response) implements ReportLlmClient {

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public Optional<String> generateJson(String prompt) {
            return response;
        }
    }
}
