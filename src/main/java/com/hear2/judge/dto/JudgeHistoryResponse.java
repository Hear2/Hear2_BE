package com.hear2.judge.dto;

import com.hear2.emotion.enums.RiskLevel;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.support.JudgeParticipantFormatter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
@Schema(description = "AI 판사 판결 이력 응답")
public class JudgeHistoryResponse {

    @Schema(description = "판결 이력 ID", example = "7")
    private Long id;

    @Schema(description = "커플 ID", example = "1")
    private Long coupleId;

    @Schema(description = "판결을 유도한 채팅 메시지 ID", example = "100")
    private Long triggerMessageId;

    @Schema(description = "판결을 유도한 메시지의 리스크 단계", example = "WARNING")
    private RiskLevel triggerRiskLevel;

    @Schema(description = "판결문에 공통 노출되는 첫 번째 참여자 입장 요약")
    private String summaryA;

    @Schema(description = "판결문에 공통 노출되는 두 번째 참여자 입장 요약")
    private String summaryB;

    @Schema(description = "현재 로그인 사용자의 표시 이름. 이름이 없으면 '나'가 반환됩니다.", example = "승현")
    private String userName;

    @Schema(description = "상대방의 표시 이름. 이름이 없으면 '상대방'이 반환됩니다.", example = "지민")
    private String partnerName;

    @Schema(description = "판결문")
    private String judgement;

    @Schema(description = "화해 방안")
    private String solution;

    @Schema(description = "바로 전송할 수 있는 화해 메시지")
    private String reconciliationMessage;

    @Schema(description = "판결 요청자용 화해 메시지")
    private String requestedReconciliationMessage;

    @Schema(description = "상대방용 화해 메시지")
    private String partnerReconciliationMessage;

    @Schema(description = "갈등 유형", example = "COMMUNICATION")
    private ConflictType conflictType;

    @Schema(description = "판결 톤", example = "WITTY")
    private JudgeTone judgeTone;

    @Schema(description = "생성 시각", example = "2026-05-09T16:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "현재 로그인 사용자가 이 판결문에 피드백을 남겼는지 여부", example = "true")
    private Boolean feedbackSubmitted;

    @Schema(description = "현재 로그인 사용자의 만족 여부. 피드백이 없으면 null입니다.", example = "false", nullable = true)
    private Boolean satisfied;

    @Schema(description = "현재 로그인 사용자의 추가 의견. 피드백이 없거나 의견이 없으면 null입니다.", example = "조금 더 구체적인 해결책이 있었으면 좋겠어요.", nullable = true)
    private String feedbackText;

    public static JudgeHistoryResponse from(
            JudgeHistory history,
            Long currentUserId,
            String userName,
            String partnerName,
            String summaryAName,
            String summaryBName
    ) {
        return from(
                history,
                JudgeFeedbackSummary.empty(),
                currentUserId,
                userName,
                partnerName,
                summaryAName,
                summaryBName
        );
    }

    public static JudgeHistoryResponse from(
            JudgeHistory history,
            JudgeFeedbackSummary feedback,
            Long currentUserId,
            String userName,
            String partnerName,
            String summaryAName,
            String summaryBName
    ) {
        return JudgeHistoryResponse.builder()
                .id(history.getId())
                .coupleId(history.getCoupleId())
                .triggerMessageId(history.getTriggerMessageId())
                .triggerRiskLevel(history.getTriggerRiskLevel())
                .summaryA(JudgeParticipantFormatter.formatSharedSummary(history.getSummaryA(), summaryAName))
                .summaryB(JudgeParticipantFormatter.formatSharedSummary(history.getSummaryB(), summaryBName))
                .userName(userName)
                .partnerName(partnerName)
                .judgement(history.getJudgement())
                .solution(history.getSolution())
                .reconciliationMessage(resolveReconciliationMessage(history, currentUserId))
                .requestedReconciliationMessage(history.getRequestedReconciliationMessage())
                .partnerReconciliationMessage(history.getPartnerReconciliationMessage())
                .conflictType(history.getConflictType())
                .judgeTone(history.getJudgeTone())
                .createdAt(history.getCreatedAt())
                .feedbackSubmitted(feedback.feedbackSubmitted())
                .satisfied(feedback.satisfied())
                .feedbackText(feedback.feedbackText())
                .build();
    }

    private static String resolveReconciliationMessage(JudgeHistory history, Long currentUserId) {
        if (Objects.equals(currentUserId, history.getRequestedByUserId())
                && hasText(history.getRequestedReconciliationMessage())) {
            return history.getRequestedReconciliationMessage().trim();
        }
        if (Objects.equals(currentUserId, history.getPartnerUserId())
                && hasText(history.getPartnerReconciliationMessage())) {
            return history.getPartnerReconciliationMessage().trim();
        }
        if (hasText(history.getRequestedReconciliationMessage())) {
            return history.getRequestedReconciliationMessage().trim();
        }
        if (hasText(history.getPartnerReconciliationMessage())) {
            return history.getPartnerReconciliationMessage().trim();
        }
        return history.getReconciliationMessage();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
