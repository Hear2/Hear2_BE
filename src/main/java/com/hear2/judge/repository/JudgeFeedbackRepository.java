package com.hear2.judge.repository;

import com.hear2.judge.entity.JudgeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JudgeFeedbackRepository extends JpaRepository<JudgeFeedback, Long> {

    Optional<JudgeFeedback> findByJudgeHistoryIdAndUserId(Long judgeHistoryId, Long userId);

    List<JudgeFeedback> findByJudgeHistoryIdInAndUserId(Collection<Long> judgeHistoryIds, Long userId);
}
