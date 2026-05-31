package com.hear2.qna.repository;

import com.hear2.qna.entity.DailyQuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyQuestionTemplateRepository extends JpaRepository<DailyQuestionTemplate, Long> {

    Optional<DailyQuestionTemplate> findByDayIndex(Integer dayIndex);

    Optional<DailyQuestionTemplate> findFirstByOrderByDayIndexAsc();

    boolean existsByDayIndex(Integer dayIndex);
}
