package com.hear2.judge.dto;

import com.hear2.emotion.enums.RiskLevel;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    @Schema(description = "A측 입장 요약")
    private String summaryA;

    @Schema(description = "B측 입장 요약")
    private String summaryB;

    @Schema(description = "판결문")
    private String judgement;

    @Schema(description = "화해 방안")
    private String solution;

    @Schema(description = "바로 전송할 수 있는 화해 메시지")
    private String reconciliationMessage;

    @Schema(description = "갈등 유형", example = "COMMUNICATION")
    private ConflictType conflictType;

    @Schema(description = "판결 톤", example = "WITTY")
    private JudgeTone judgeTone;

    @Schema(description = "생성 시각", example = "2026-05-09T16:30:00")
    private LocalDateTime createdAt;

    public static JudgeHistoryResponse from(JudgeHistory history) {
        return JudgeHistoryResponse.builder()
                .id(history.getId())
                .coupleId(history.getCoupleId())
                .triggerMessageId(history.getTriggerMessageId())
                .triggerRiskLevel(history.getTriggerRiskLevel())
                .summaryA(history.getSummaryA())
                .summaryB(history.getSummaryB())
                .judgement(history.getJudgement())
                .solution(history.getSolution())
                .reconciliationMessage(history.getReconciliationMessage())
                .conflictType(history.getConflictType())
                .judgeTone(history.getJudgeTone())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
