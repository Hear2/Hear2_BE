package com.hear2.judge.dto;

import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JudgeFastApiMessage {

    private Long messageId;

    private Long senderId;

    private String content;

    private EmotionType emotionType;

    private Double emotionScore;

    private Double negativeScore;

    private String emotionEmoji;

    private RiskLevel riskLevel;

    private LocalDateTime createdAt;
}
