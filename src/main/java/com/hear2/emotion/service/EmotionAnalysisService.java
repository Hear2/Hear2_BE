package com.hear2.emotion.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.emotion.client.EmotionAnalysisClient;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.dto.RiskAnalysisResponse;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.notification.service.FcmNotificationService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final EmotionAnalysisClient emotionAnalysisClient;
    private final RiskDetectionService riskDetectionService;
    private final FcmNotificationService fcmNotificationService;
    private final ChatParticipantResolver chatParticipantResolver;

    @Transactional(noRollbackFor = Exception.class)
    public EmotionAnalysisResponse analyzeAndSave(ChatMessage message) {
        EmotionAnalysisResponse response = analyzeContent(message.getId(), message.getContent());

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
        try {
            fcmNotificationService.sendRiskAlert(message, response);
        } catch (Exception exception) {
            log.warn("Failed to send risk alert after emotion analysis. messageId={}", message.getId(), exception);
        }

        return response;
    }

    public EmotionAnalysisResponse analyzeMessageForUser(Long currentUserId, Long messageId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }

        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));
        if (!context.coupleId().equals(message.getCoupleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "message does not belong to your couple");
        }

        return emotionAnalysisRepository.findByMessageId(messageId)
                .map(this::toResponse)
                .orElseGet(() -> analyzeAndSave(message));
    }

    private EmotionAnalysisResponse analyzeContent(Long messageId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required for emotion analysis");
        }

        EmotionAnalysisResponse analyzed = emotionAnalysisClient.analyze(messageId, content);
        RiskAnalysisResponse risk = riskDetectionService.analyze(content, analyzed);

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
