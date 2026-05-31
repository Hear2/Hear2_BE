package com.hear2.qna.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "daily_question_template",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily_question_template_day_index", columnNames = "day_index")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyQuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;
}
