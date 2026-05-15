package com.hear2.qna.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_question_couple_date",
                        columnNames = {"couple_id", "question_date"}
                )
        },
        indexes = {
                @Index(name = "idx_daily_question_couple", columnList = "couple_id"),
                @Index(name = "idx_daily_question_template", columnList = "template_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    @Builder.Default
    @Column(name = "both_answered", nullable = false)
    @ColumnDefault("false")
    private Boolean bothAnswered = false;

    @Column(name = "both_answered_at")
    private LocalDateTime bothAnsweredAt;

    @Builder.Default
    @Column(name = "qna_reward_granted", nullable = false)
    @ColumnDefault("false")
    private Boolean qnaRewardGranted = false;

    @Column(name = "qna_reward_granted_at")
    private LocalDateTime qnaRewardGrantedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.bothAnswered == null) {
            this.bothAnswered = false;
        }
        if (this.qnaRewardGranted == null) {
            this.qnaRewardGranted = false;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void markBothAnswered(LocalDateTime bothAnsweredAt) {
        if (Boolean.TRUE.equals(this.bothAnswered)) {
            return;
        }
        this.bothAnswered = true;
        this.bothAnsweredAt = bothAnsweredAt;
    }
}
