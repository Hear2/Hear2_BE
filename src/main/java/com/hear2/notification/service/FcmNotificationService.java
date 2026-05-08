package com.hear2.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.notification.config.FcmProperties;
import com.hear2.notification.entity.FcmToken;
import com.hear2.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final FcmProperties fcmProperties;
    private final FcmTokenRepository fcmTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public void sendRiskAlert(ChatMessage message, EmotionAnalysisResponse emotion) {
        if (!shouldSend(emotion)) {
            return;
        }

        if (!fcmProperties.isEnabled()) {
            log.debug("FCM is disabled. risk alert skipped. messageId={}", message.getId());
            return;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging bean is not available. risk alert skipped. messageId={}", message.getId());
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndActiveTrue(message.getReceiverId());
        if (tokens.isEmpty()) {
            log.debug("Receiver has no active FCM token. receiverId={}, messageId={}",
                    message.getReceiverId(), message.getId());
            return;
        }

        for (FcmToken fcmToken : tokens) {
            sendToToken(firebaseMessaging, fcmToken, message, emotion);
        }
    }

    private boolean shouldSend(EmotionAnalysisResponse emotion) {
        return emotion != null
                && Boolean.TRUE.equals(emotion.getRiskDetected())
                && emotion.getRiskLevel() != null
                && emotion.getRiskLevel().isRisk();
    }

    private void sendToToken(
            FirebaseMessaging firebaseMessaging,
            FcmToken fcmToken,
            ChatMessage message,
            EmotionAnalysisResponse emotion
    ) {
        try {
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(notificationTitle(emotion.getRiskLevel()))
                            .setBody(notificationBody(emotion))
                            .build())
                    .putAllData(notificationData(message, emotion))
                    .build();

            firebaseMessaging.send(fcmMessage);
        } catch (FirebaseMessagingException ex) {
            log.warn("FCM risk alert send failed. tokenId={}, receiverId={}, messageId={}",
                    fcmToken.getId(), message.getReceiverId(), message.getId(), ex);
        }
    }

    private String notificationTitle(RiskLevel riskLevel) {
        return "[" + riskLevel.getDisplayName() + "] 대화 리스크 감지";
    }

    private String notificationBody(EmotionAnalysisResponse emotion) {
        return switch (emotion.getRiskLevel()) {
            case CAUTION -> "상대방의 메시지에서 주의가 필요한 감정 신호가 감지됐어요.";
            case WARNING -> "상대방의 메시지에서 경고 수준의 감정 신호가 감지됐어요.";
            case DANGER -> "상대방의 메시지에서 위험 수준의 신호가 감지됐어요.";
            case NONE -> "대화 리스크 알림입니다.";
        };
    }

    private Map<String, String> notificationData(ChatMessage message, EmotionAnalysisResponse emotion) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT_RISK_ALERT");
        data.put("messageId", stringValue(message.getId()));
        data.put("coupleId", stringValue(message.getCoupleId()));
        data.put("senderId", stringValue(message.getSenderId()));
        data.put("receiverId", stringValue(message.getReceiverId()));
        data.put("riskLevel", stringValue(emotion.getRiskLevel()));
        data.put("emotionType", stringValue(emotion.getEmotionType()));
        data.put("emotionScore", stringValue(emotion.getEmotionScore()));
        data.put("negativeScore", stringValue(emotion.getNegativeScore()));
        data.put("emotionEmoji", stringValue(emotion.getEmotionEmoji()));
        data.put("riskReason", stringValue(emotion.getRiskReason()));

        if (!CollectionUtils.isEmpty(emotion.getDetectedRiskKeywords())) {
            data.put("detectedRiskKeywords", String.join(",", emotion.getDetectedRiskKeywords()));
        }

        return data;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
