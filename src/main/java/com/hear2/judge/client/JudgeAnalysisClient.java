package com.hear2.judge.client;

import com.hear2.emotion.config.EmotionAnalysisProperties;
import com.hear2.judge.dto.JudgeFastApiRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Component
public class JudgeAnalysisClient {

    private final EmotionAnalysisProperties properties;
    private final RestTemplate restTemplate;

    public JudgeAnalysisClient(EmotionAnalysisProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public JudgeResponse requestJudgement(JudgeFastApiRequest request) {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI judge analysis is disabled");
        }

        try {
            JudgeResponse response = restTemplate.postForObject(
                    judgeUri(),
                    request,
                    JudgeResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge analysis returned empty response");
            }

            return normalize(response);
        } catch (RestClientException ex) {
            log.warn("FastAPI judge analysis request failed. coupleId={}, triggerMessageId={}",
                    request.getCoupleId(), request.getTriggerMessageId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge analysis request failed", ex);
        }
    }

    private URI judgeUri() {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(properties.getJudgePath())
                .build()
                .toUri();
    }

    private JudgeResponse normalize(JudgeResponse response) {
        if (!StringUtils.hasText(response.getSummaryA())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge response missing summaryA");
        }
        if (!StringUtils.hasText(response.getSummaryB())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge response missing summaryB");
        }
        if (!StringUtils.hasText(response.getJudgement())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge response missing judgement");
        }
        if (!StringUtils.hasText(response.getSolution())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI judge response missing solution");
        }

        String reconciliationMessage = StringUtils.hasText(response.getReconciliationMessage())
                ? response.getReconciliationMessage().trim()
                : response.getSolution().trim();

        return JudgeResponse.builder()
                .cardType("AI_JUDGE")
                .summaryA(response.getSummaryA().trim())
                .summaryB(response.getSummaryB().trim())
                .judgement(response.getJudgement().trim())
                .solution(response.getSolution().trim())
                .reconciliationMessage(reconciliationMessage)
                .conflictType(response.getConflictType() == null ? ConflictType.OTHER : response.getConflictType())
                .judgeTone(response.getJudgeTone() == null ? JudgeTone.WITTY : response.getJudgeTone())
                .build();
    }
}
