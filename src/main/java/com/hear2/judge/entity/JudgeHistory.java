package com.hear2.judge.entity;

import com.hear2.emotion.enums.RiskLevel;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_judge_history_couple_created", columnList = "coupleId, createdAt"),
        @Index(name = "idx_judge_history_couple_conflict", columnList = "coupleId, conflictType")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long coupleId;

    private Long triggerMessageId;

    @Enumerated(EnumType.STRING)
    private RiskLevel triggerRiskLevel;

    @Column(columnDefinition = "TEXT")
    private String summaryA;

    @Column(columnDefinition = "TEXT")
    private String summaryB;

    @Column(columnDefinition = "TEXT")
    private String judgement;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(columnDefinition = "TEXT")
    private String reconciliationMessage;

    @Enumerated(EnumType.STRING)
    private ConflictType conflictType;

    @Enumerated(EnumType.STRING)
    private JudgeTone judgeTone;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
