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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_answer_question_user",
                        columnNames = {"question_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_daily_answer_question", columnList = "question_id"),
                @Index(name = "idx_daily_answer_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    void onCreate() {
        if (this.answeredAt == null) {
            this.answeredAt = LocalDateTime.now();
        }
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
    }
}
