package com.hear2.emotion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hear2.chat.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "감정 분석 요청")
public class EmotionAnalysisRequest {

    @Schema(description = "분석 대상 메시지 ID", example = "100")
    @JsonProperty("message_id")
    @JsonAlias("messageId")
    private Long messageId;

    @Schema(description = "커플 ID", example = "1")
    @JsonProperty("couple_id")
    @JsonAlias("coupleId")
    private Long coupleId;

    @Schema(description = "발신자 ID", example = "10")
    @JsonProperty("sender_id")
    @JsonAlias("senderId")
    private Long senderId;

    @Schema(description = "수신자 ID", example = "11")
    @JsonProperty("receiver_id")
    @JsonAlias("receiverId")
    private Long receiverId;

    @Schema(description = "감정 분석할 메시지 내용", example = "너 때문에 너무 화나")
    private String content;

    public static EmotionAnalysisRequest from(ChatMessage message) {
        return EmotionAnalysisRequest.builder()
                .messageId(message.getId())
                .coupleId(message.getCoupleId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .build();
    }
}
