package com.hear2.emotion.client;

import com.hear2.emotion.config.EmotionAnalysisProperties;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class EmotionAnalysisClient {

    private final EmotionAnalysisProperties properties;
    private final RestTemplate restTemplate;

    public EmotionAnalysisClient(EmotionAnalysisProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public EmotionAnalysisResponse analyze(Long messageId, String content) {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "emotion analysis is disabled");
        }

        try {
            EmotionAnalysisResponse response = restTemplate.postForObject(
                    analyzeUri(),
                    fastApiRequest(messageId, content),
                    EmotionAnalysisResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI emotion analysis returned empty response");
            }

            return normalize(response);
        } catch (HttpStatusCodeException ex) {
            log.warn("FastAPI emotion analysis returned error. messageId={}, status={}, body={}",
                    messageId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FastAPI emotion analysis returned error: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            log.warn("FastAPI emotion analysis request failed. messageId={}", messageId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI emotion analysis request failed", ex);
        }
    }

    private URI analyzeUri() {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(properties.getAnalyzePath())
                .build()
                .toUri();
    }

    private Map<String, Object> fastApiRequest(Long messageId, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", content);
        body.put("content", content);
        body.put("message_id", messageId);
        return body;
    }

    private EmotionAnalysisResponse normalize(EmotionAnalysisResponse response) {
        EmotionType emotionType = response.getEmotionType();
        if (emotionType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI emotion analysis response missing emotionType");
        }

        double emotionScore = requiredScore(response.getEmotionScore(), "emotionScore");
        Double negativeScore = response.getNegativeScore() == null
                ? null
                : requiredScore(response.getNegativeScore(), "negativeScore");
        String emotionEmoji = StringUtils.hasText(response.getEmotionEmoji())
                ? response.getEmotionEmoji()
                : emotionType.getEmoji();

        return EmotionAnalysisResponse.builder()
                .emotionType(emotionType)
                .emotionScore(emotionScore)
                .negativeScore(negativeScore)
                .emotionEmoji(emotionEmoji)
                .riskLevel(response.getRiskLevel())
                .riskDetected(response.getRiskDetected())
                .riskReason(response.getRiskReason())
                .detectedRiskKeywords(response.getDetectedRiskKeywords())
                .build();
    }

    private double requiredScore(Double value, String fieldName) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FastAPI emotion analysis response missing valid " + fieldName);
        }

        return clamp(value);
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
