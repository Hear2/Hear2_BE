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

    @Schema(description = "감정 유형", example = "HAPPY", allowableValues = {"HAPPY", "SAD", "ANGRY", "ANXIOUS", "NEUTRAL"})
    private EmotionType emotionType;

    @Schema(description = "감정 점수. 0.0부터 1.0까지의 값입니다.", example = "0.91")
    private Double emotionScore;

    @Schema(description = "부정 감정 점수. 0.0부터 1.0까지의 값입니다.", example = "0.12")
    private Double negativeScore;

    @Schema(description = "말풍선 옆에 표시할 감정 이모지", example = "😊")
    private String emotionEmoji;

    @Schema(description = "리스크 단계", example = "NONE", allowableValues = {"NONE", "CAUTION", "WARNING", "DANGER"})
    private RiskLevel riskLevel;

    @Schema(description = "리스크 감지 여부", example = "false")
    private Boolean riskDetected;

    @Schema(description = "리스크 감지 사유", example = "risk keyword detected")
    private String riskReason;

    @Schema(description = "감지된 위험 키워드 목록", example = "[\"가만 안 둬\"]")
    private List<String> detectedRiskKeywords;

    @Schema(description = "AI 판사 호출 버튼 노출 여부. WARNING/DANGER 리스크이거나 부정 감정 점수가 높을 때 true입니다.", example = "true")
    private Boolean judgeAvailable;

    @Schema(description = "AI 판사 호출 시 triggerMessageId로 전달할 메시지 ID", example = "100")
    private Long judgeTriggerMessageId;

    public static ChatMessageResponse from(ChatMessage message) {
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
                .judgeAvailable(false)
                .build();
    }

    public static ChatMessageResponse from(
            ChatMessage message,
            EmotionAnalysisResponse emotion
    ) {
        if (emotion == null) {
            return from(message);
        }

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

                .emotionType(emotion.getEmotionType())
                .emotionScore(emotion.getEmotionScore())
                .negativeScore(emotion.getNegativeScore())
                .emotionEmoji(emotion.getEmotionEmoji())
                .riskLevel(emotion.getRiskLevel())
                .riskDetected(emotion.getRiskDetected())
                .riskReason(emotion.getRiskReason())
                .detectedRiskKeywords(emotion.getDetectedRiskKeywords())
                .judgeAvailable(isJudgeAvailable(emotion))
                .judgeTriggerMessageId(isJudgeAvailable(emotion) ? message.getId() : null)
                .build();
    }

    private static boolean isJudgeAvailable(EmotionAnalysisResponse emotion) {
        return JudgeTriggerPolicy.isJudgeAvailable(emotion.getRiskLevel(), emotion.getNegativeScore());
    }
}
