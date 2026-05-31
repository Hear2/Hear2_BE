package com.hear2.qna.repository;

import com.hear2.qna.entity.DailyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {

    Optional<DailyQuestion> findByCoupleIdAndQuestionDate(Long coupleId, LocalDate questionDate);

    Optional<DailyQuestion> findByQuestionIdAndCoupleId(Long questionId, Long coupleId);

    Optional<DailyQuestion> findFirstByCoupleIdOrderByQuestionDateDescQuestionIdDesc(Long coupleId);

    List<DailyQuestion> findByCoupleIdOrderByQuestionDateDescQuestionIdDesc(Long coupleId);

    List<DailyQuestion> findByQuestionIdIn(Collection<Long> questionIds);

    long countByCoupleId(Long coupleId);
}
