package com.hear2.qna.repository;

import com.hear2.qna.entity.DailyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    Optional<DailyAnswer> findByQuestionIdAndUserId(Long questionId, Long userId);

    List<DailyAnswer> findByQuestionIdIn(Collection<Long> questionIds);

    List<DailyAnswer> findByQuestionIdInAndUserId(Collection<Long> questionIds, Long userId);

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndAnsweredAtBefore(Long questionId, LocalDateTime answeredAt);

    @Query("select count(distinct answer.userId) from DailyAnswer answer where answer.questionId = :questionId")
    long countDistinctUserIdsByQuestionId(@Param("questionId") Long questionId);
}
