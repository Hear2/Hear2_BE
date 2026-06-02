package com.hear2.chat.dto;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageResponseTest {

    @Test
    void returnsNullEmotionFieldsWhenEmotionAnalysisIsMissing() {
        ChatMessage message = textMessage(100L);

        ChatMessageResponse response = ChatMessageResponse.from(message);

        assertThat(response.getEmotionType()).isNull();
        assertThat(response.getEmotionScore()).isNull();
        assertThat(response.getEmotionEmoji()).isNull();
        assertThat(response.getRiskLevel()).isNull();
        assertThat(response.getJudgeAvailable()).isFalse();
        assertThat(response.getJudgeTriggerMessageId()).isNull();
        assertThat(response.getEmotionFeedback()).isNull();
    }

    @Test
    void exposesJudgeButtonForWarningRiskMessage() {
        ChatMessage message = textMessage(100L);
        EmotionAnalysisResponse emotion = emotion(RiskLevel.WARNING);

        ChatMessageResponse response = ChatMessageResponse.from(message, emotion);

        assertThat(response.getJudgeAvailable()).isTrue();
        assertThat(response.getJudgeTriggerMessageId()).isEqualTo(100L);
        assertThat(response.getEmotionFeedback()).isNull();
    }

    @Test
    void hidesJudgeButtonForCautionRiskMessage() {
        ChatMessage message = textMessage(100L);
        EmotionAnalysisResponse emotion = emotion(RiskLevel.CAUTION);

        ChatMessageResponse response = ChatMessageResponse.from(message, emotion);

        assertThat(response.getJudgeAvailable()).isFalse();
        assertThat(response.getJudgeTriggerMessageId()).isNull();
        assertThat(response.getEmotionFeedback()).isNull();
    }

    @Test
    void exposesJudgeButtonForHighNegativeScoreMessage() {
        ChatMessage message = textMessage(100L);
        EmotionAnalysisResponse emotion = emotion(RiskLevel.CAUTION, 0.72);

        ChatMessageResponse response = ChatMessageResponse.from(message, emotion);

        assertThat(response.getJudgeAvailable()).isTrue();
        assertThat(response.getJudgeTriggerMessageId()).isEqualTo(100L);
    }

    @Test
    void exposesEmotionFeedbackWhenPresent() {
        ChatMessage message = textMessage(100L);
        EmotionAnalysisResponse emotion = emotion(RiskLevel.NONE);

        ChatMessageResponse response = ChatMessageResponse.from(message, emotion, true);

        assertThat(response.getEmotionFeedback()).isTrue();
    }

    private ChatMessage textMessage(Long id) {
        return ChatMessage.builder()
                .id(id)
                .coupleId(1L)
                .senderId(10L)
                .receiverId(11L)
                .content("왜 이렇게 답장이 늦어?")
                .messageType(MessageType.TEXT)
                .build();
    }

    private EmotionAnalysisResponse emotion(RiskLevel riskLevel) {
        return emotion(riskLevel, null);
    }

    private EmotionAnalysisResponse emotion(RiskLevel riskLevel, Double negativeScore) {
        return EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.82)
                .negativeScore(negativeScore)
                .riskLevel(riskLevel)
                .riskDetected(riskLevel.isRisk())
                .build();
    }
}
