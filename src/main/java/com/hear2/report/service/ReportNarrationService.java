package com.hear2.report.service;

import com.hear2.emotion.enums.EmotionType;
import com.hear2.report.dto.EmotionFlowPointResponse;
import com.hear2.report.support.ReportPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportNarrationService {

    public String buildFallbackSummary(
            ReportPeriod period,
            List<EmotionFlowPointResponse> emotionFlow,
            long totalConversationCount,
            int positiveRatio,
            long conflictCount,
            EmotionType dominantEmotionType
    ) {
        EmotionFlowPointResponse peakPoint = emotionFlow.stream()
                .max(Comparator.comparingInt(EmotionFlowPointResponse::getPositiveRatio)
                        .thenComparingLong(EmotionFlowPointResponse::getMessageCount))
                .orElse(null);

        String periodPrefix = period.getReportType().name().equals("MONTHLY") ? "이번 달은" : "이번 주는";
        String peakSentence = buildPeakSentence(periodPrefix, peakPoint);
        String toneSentence = buildToneSentence(positiveRatio, dominantEmotionType);
        String activitySentence = buildActivitySentence(totalConversationCount, conflictCount);

        return String.join(" ", List.of(peakSentence, toneSentence, activitySentence))
                .trim();
    }

    private String buildPeakSentence(String prefix, EmotionFlowPointResponse peakPoint) {
        if (peakPoint == null || !StringUtils.hasText(peakPoint.getLabel())) {
            return prefix + " 서로의 감정을 천천히 쌓아간 흐름이 보였어요.";
        }

        return prefix + " " + peakPoint.getLabel() + "에 감정이 가장 풍부했어요.";
    }

    private String buildToneSentence(int positiveRatio, EmotionType dominantEmotionType) {
        if (positiveRatio >= 75) {
            return "전체 분위기도 꽤 다정하고 안정적이었네요.";
        }
        if (positiveRatio >= 55) {
            return "좋은 순간과 조심할 순간이 균형 있게 섞여 있었어요.";
        }
        if (dominantEmotionType != null && dominantEmotionType.isNegative()) {
            return "서로의 마음을 조금 더 천천히 확인해볼 필요가 있어 보여요.";
        }
        return "대화의 결을 조금 더 부드럽게 맞춰보면 더 좋아질 수 있어요.";
    }

    private String buildActivitySentence(long totalConversationCount, long conflictCount) {
        if (conflictCount > 0) {
            return "AI 판사가 포착한 민감한 대화는 " + conflictCount + "번 있었어요.";
        }
        if (totalConversationCount >= 100) {
            return "대화를 자주 주고받은 덕분에 관계의 온도도 선명하게 드러났어요.";
        }
        if (totalConversationCount > 0) {
            return "짧더라도 꾸준히 이어진 대화가 관계 리듬을 잘 만들고 있었어요.";
        }
        return "아직 분석할 대화가 많지 않아 다음 리포트에서 더 선명한 흐름이 잡힐 거예요.";
    }
}
