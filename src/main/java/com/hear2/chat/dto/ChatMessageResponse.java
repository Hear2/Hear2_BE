package com.hear2.chat.dto;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.judge.support.JudgeTriggerPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "100")
    private Long id;

    @Schema(description = "커플 ID", example = "1")
    private Long coupleId;

    @Schema(description = "발신자 ID", example = "10")
    private Long senderId;

    @Schema(description = "수신자 ID", example = "11")
    private Long receiverId;

    @Schema(description = "메시지 내용", example = "오늘 너무 고마웠어")
    private String content;

    @Schema(description = "메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "VIDEO"})
    private MessageType messageType;

    @Schema(description = "미디어 URL", example = "/uploads/chat/sample.png")
    private String mediaUrl;

    @Schema(description = "원본 파일명", example = "sample.png")
    private String originalFileName;

    @Schema(description = "미디어 MIME 타입", example = "image/png")
    private String mediaContentType;

    @Schema(description = "미디어 파일 크기(byte)", example = "204800")
    private Long mediaSize;

    @Schema(description = "읽은 시각", example = "2026-05-07T01:10:00")
    private LocalDateTime readAt;

    @Schema(description = "안 읽은 메시지 수", example = "1")
    private Integer unreadCount;

    @Schema(description = "메시지 생성 시각", example = "2026-05-07T01:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "감정 유형. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "HAPPY", allowableValues = {"HAPPY", "SAD", "ANGRY", "ANXIOUS", "NEUTRAL"}, nullable = true)
    private EmotionType emotionType;

    @Schema(description = "감정 점수. 0.0부터 1.0까지의 값입니다. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "0.91", nullable = true)
    private Double emotionScore;

    @Schema(description = "부정 감정 점수. 0.0부터 1.0까지의 값입니다. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "0.12", nullable = true)
    private Double negativeScore;

    @Schema(description = "말풍선 옆에 표시할 감정 이모지. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "😊", nullable = true)
    private String emotionEmoji;

    @Schema(description = "리스크 단계. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "NONE", allowableValues = {"NONE", "CAUTION", "WARNING", "DANGER"}, nullable = true)
    private RiskLevel riskLevel;

    @Schema(description = "리스크 감지 여부. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "false", nullable = true)
    private Boolean riskDetected;

    @Schema(description = "리스크 감지 사유. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "risk keyword detected", nullable = true)
    private String riskReason;

    @Schema(description = "감지된 위험 키워드 목록. 메시지 저장 직후 즉시 분석에 성공하면 채워지고, 분석 실패 시 null일 수 있습니다.", example = "[\"가만 안 둬\"]", nullable = true)
    private List<String> detectedRiskKeywords;

    @Schema(description = "AI 판사 호출 버튼 노출 여부. 감정 분석이 완료되면 WARNING/DANGER 리스크이거나 부정 감정 점수가 높을 때 true입니다. 분석 전/실패 시 false입니다.", example = "true")
    private Boolean judgeAvailable;

    @Schema(description = "AI 판사 호출 시 triggerMessageId로 전달할 메시지 ID. 감정 분석이 완료되어 AI 판사 호출 가능 조건을 만족할 때만 값이 채워집니다.", example = "100", nullable = true)
    private Long judgeTriggerMessageId;

    @Schema(description = "현재 로그인 사용자의 감정 분석 피드백. 아직 피드백하지 않았으면 null, 맞아요면 true, 아니에요면 false입니다.", example = "true", nullable = true)
    private Boolean emotionFeedback;

    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null, null);
    }

    public static ChatMessageResponse from(ChatMessage message, EmotionAnalysisResponse emotion) {
        return from(message, emotion, null);
    }

    public static ChatMessageResponse from(
            ChatMessage message,
            EmotionAnalysisResponse emotion,
            Boolean emotionFeedback
    ) {
        boolean judgeAvailable = isJudgeAvailable(emotion);

        return ChatMessageResponse.builder()
                .id(message.getId())
                .coupleId(message.getCoupleId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .mediaUrl(message.getMediaUrl())
                .originalFileName(message.getOriginalFileName())
                .mediaContentType(message.getMediaContentType())
                .mediaSize(message.getMediaSize())
                .readAt(message.getReadAt())
                .unreadCount(message.getReadAt() == null ? 1 : 0)
                .createdAt(message.getCreatedAt())
                .emotionType(emotion == null ? null : emotion.getEmotionType())
                .emotionScore(emotion == null ? null : emotion.getEmotionScore())
                .negativeScore(emotion == null ? null : emotion.getNegativeScore())
                .emotionEmoji(emotion == null ? null : emotion.getEmotionEmoji())
                .riskLevel(emotion == null ? null : emotion.getRiskLevel())
                .riskDetected(emotion == null ? null : emotion.getRiskDetected())
                .riskReason(emotion == null ? null : emotion.getRiskReason())
                .detectedRiskKeywords(emotion == null ? null : emotion.getDetectedRiskKeywords())
                .judgeAvailable(judgeAvailable)
                .judgeTriggerMessageId(judgeAvailable ? message.getId() : null)
                .emotionFeedback(emotionFeedback)
                .build();
    }

    private static boolean isJudgeAvailable(EmotionAnalysisResponse emotion) {
        if (emotion == null) {
            return false;
        }
        return JudgeTriggerPolicy.isJudgeAvailable(emotion.getRiskLevel(), emotion.getNegativeScore());
    }
}
