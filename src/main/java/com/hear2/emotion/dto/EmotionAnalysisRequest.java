package com.hear2.emotion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
}
