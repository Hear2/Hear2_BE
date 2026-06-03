package com.hear2.judge.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.support.JudgeParticipantFormatter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "AI 판사 카드 응답")
public class JudgeResponse {

    @Builder.Default
    @Schema(description = "프론트 카드 렌더링 타입", example = "AI_JUDGE")
    private String cardType = "AI_JUDGE";

    @Schema(description = "판결 이력 ID", example = "7")
    private Long historyId;

    @Schema(description = "커플 ID", example = "1")
    private Long coupleId;

    @Schema(description = "AI 판사 호출을 유도한 채팅 메시지 ID", example = "100")
    private Long triggerMessageId;

    @Schema(description = "호출을 유도한 메시지의 리스크 단계", example = "WARNING")
    private RiskLevel triggerRiskLevel;

    @JsonAlias({"summary_a", "summaryA"})
    @Schema(description = "판결문에 공통 노출되는 첫 번째 참여자 입장 요약", example = "승현은 답장이 늦어져 서운함을 느꼈습니다.")
    private String summaryA;

    @JsonAlias({"summary_b", "summaryB"})
    @Schema(description = "판결문에 공통 노출되는 두 번째 참여자 입장 요약", example = "지민은 바쁜 상황을 설명했지만 표현이 충분하지 않았습니다.")
    private String summaryB;

    @Schema(description = "현재 로그인 사용자의 표시 이름. 이름이 없으면 '나'가 반환됩니다.", example = "승현")
    private String userName;

    @Schema(description = "상대방의 표시 이름. 이름이 없으면 '상대방'이 반환됩니다.", example = "지민")
    private String partnerName;

    @JsonAlias({"judgment", "judgement"})
    @Schema(description = "위트 있는 판결문", example = "본 재판부는 두 사람 모두 말보다 마음이 앞섰다고 판결합니다.")
    private String judgement;

    @JsonAlias({"reconciliation", "solution"})
    @Schema(description = "화해 방안", example = "오늘은 10분만 서로의 말을 끊지 않고 들어주세요.")
    private String solution;

    @JsonAlias({"reconciliation_message", "reconciliationMessage", "apologyMessage", "peaceMessage"})
    @Schema(description = "바로 전송할 수 있는 화해 메시지", example = "아까 말이 날카로웠어. 네 하루가 어땠는지 먼저 듣고 싶어.")
    private String reconciliationMessage;

    @JsonAlias({"requested_reconciliation_message", "requestedReconciliationMessage"})
    @Schema(description = "판결 요청자용 화해 메시지", example = "아까 내 말이 너무 날카로웠어. 왜 서운했는지 차분히 다시 듣고 싶어.")
    private String requestedReconciliationMessage;

    @JsonAlias({"partner_reconciliation_message", "partnerReconciliationMessage"})
    @Schema(description = "상대방용 화해 메시지", example = "내가 바로 설명부터 하느라 네 감정을 놓쳤어. 어떤 부분이 가장 속상했는지 먼저 듣고 싶어.")
    private String partnerReconciliationMessage;

    @JsonAlias({"conflict_type", "conflictType"})
    @Schema(description = "갈등 유형", example = "COMMUNICATION")
    private ConflictType conflictType;

    @JsonAlias({"judge_tone", "judgeTone"})
    @Schema(description = "판결 톤", example = "WITTY")
    private JudgeTone judgeTone;

    @Schema(description = "같은 커플에서 동일 갈등 유형이 저장된 누적 횟수", example = "3")
    private Long sameConflictCount;

    @Schema(description = "판결 생성 시각", example = "2026-05-09T16:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "현재 로그인 사용자가 이 판결문에 피드백을 남겼는지 여부", example = "false")
    private Boolean feedbackSubmitted;

    @Schema(description = "현재 로그인 사용자의 만족 여부. 피드백이 없으면 null입니다.", example = "true", nullable = true)
    private Boolean satisfied;

    @Schema(description = "현재 로그인 사용자의 추가 의견. 피드백이 없거나 의견이 없으면 null입니다.", example = "상대방 입장을 잘 설명해줘서 도움이 되었어요.", nullable = true)
    private String feedbackText;

    @JsonIgnore
    public ConflictType resolvedConflictType() {
        return conflictType == null ? ConflictType.OTHER : conflictType;
    }

    @JsonIgnore
    public JudgeTone resolvedJudgeTone() {
        return judgeTone == null ? JudgeTone.WITTY : judgeTone;
    }

    public static JudgeResponse from(
            JudgeHistory history,
            Long sameConflictCount,
            Long currentUserId,
            String userName,
            String partnerName,
            String summaryAName,
            String summaryBName
    ) {
        return from(
                history,
                sameConflictCount,
                JudgeFeedbackSummary.empty(),
                currentUserId,
                userName,
                partnerName,
                summaryAName,
                summaryBName
        );
    }

    public static JudgeResponse from(
            JudgeHistory history,
            Long sameConflictCount,
            JudgeFeedbackSummary feedback,
            Long currentUserId,
            String userName,
            String partnerName,
            String summaryAName,
            String summaryBName
    ) {
        String requestedMessage = history.getRequestedReconciliationMessage();
        String partnerMessage = history.getPartnerReconciliationMessage();

        return JudgeResponse.builder()
                .cardType("AI_JUDGE")
                .historyId(history.getId())
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
                .requestedReconciliationMessage(requestedMessage)
                .partnerReconciliationMessage(partnerMessage)
                .conflictType(history.getConflictType())
                .judgeTone(history.getJudgeTone())
                .sameConflictCount(sameConflictCount)
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
