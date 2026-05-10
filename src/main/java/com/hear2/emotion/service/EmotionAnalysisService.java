package com.hear2.emotion.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.emotion.client.EmotionAnalysisClient;
import com.hear2.emotion.dto.EmotionAnalysisRequest;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.dto.RiskAnalysisResponse;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.notification.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final EmotionAnalysisClient emotionAnalysisClient;
    private final RiskDetectionService riskDetectionService;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public EmotionAnalysisResponse analyzeAndSave(ChatMessage message) {
        EmotionAnalysisResponse response = analyzeContent(EmotionAnalysisRequest.from(message));

        EmotionAnalysis analysis = EmotionAnalysis.builder()
                .message(message)
                .emotionType(response.getEmotionType())
                .emotionScore(response.getEmotionScore())
                .negativeScore(response.getNegativeScore())
                .emotionEmoji(response.getEmotionEmoji())
                .riskLevel(response.getRiskLevel())
                .riskDetected(response.getRiskDetected())
                .riskReason(response.getRiskReason())
                .detectedRiskKeywords(joinKeywords(response.getDetectedRiskKeywords()))
                .build();

        emotionAnalysisRepository.save(analysis);
        fcmNotificationService.sendRiskAlert(message, response);

        return response;
    }

    public EmotionAnalysisResponse analyzeContent(EmotionAnalysisRequest request) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required for emotion analysis");
        }

        EmotionAnalysisResponse analyzed = emotionAnalysisClient.analyze(request);
        RiskAnalysisResponse risk = riskDetectionService.analyze(request.getContent(), analyzed);

        return mergeRisk(analyzed, risk);
    }

    @Transactional(readOnly = true)
    public Map<Long, EmotionAnalysisResponse> findByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }

        return emotionAnalysisRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        analysis -> analysis.getMessage().getId(),
                        this::toResponse
                ));
    }

    private EmotionAnalysisResponse mergeRisk(EmotionAnalysisResponse analyzed, RiskAnalysisResponse risk) {
        RiskLevel riskLevel = risk.getRiskLevel() == null ? RiskLevel.NONE : risk.getRiskLevel();

        return EmotionAnalysisResponse.builder()
                .emotionType(analyzed.getEmotionType())
                .emotionScore(analyzed.getEmotionScore())
                .negativeScore(analyzed.getNegativeScore())
                .emotionEmoji(analyzed.getEmotionEmoji())
                .riskLevel(riskLevel)
                .riskDetected(risk.getRiskDetected())
                .riskReason(risk.getRiskReason())
                .detectedRiskKeywords(risk.getDetectedRiskKeywords())
                .build();
    }

    private EmotionAnalysisResponse toResponse(EmotionAnalysis analysis) {
        String emotionEmoji = StringUtils.hasText(analysis.getEmotionEmoji())
                ? analysis.getEmotionEmoji()
                : analysis.getEmotionType().getEmoji();

        return EmotionAnalysisResponse.builder()
                .emotionType(analysis.getEmotionType())
                .emotionScore(analysis.getEmotionScore())
                .negativeScore(analysis.getNegativeScore())
                .emotionEmoji(emotionEmoji)
                .riskLevel(analysis.getRiskLevel())
                .riskDetected(analysis.getRiskDetected())
                .riskReason(analysis.getRiskReason())
                .detectedRiskKeywords(splitKeywords(analysis.getDetectedRiskKeywords()))
                .build();
    }

    private String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        return String.join(",", keywords);
    }

    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }
}
