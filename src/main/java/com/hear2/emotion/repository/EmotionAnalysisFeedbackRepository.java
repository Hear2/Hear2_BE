package com.hear2.emotion.repository;

import com.hear2.emotion.entity.EmotionAnalysisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmotionAnalysisFeedbackRepository extends JpaRepository<EmotionAnalysisFeedback, Long> {

    Optional<EmotionAnalysisFeedback> findByMessageIdAndUserId(Long messageId, Long userId);

    List<EmotionAnalysisFeedback> findByMessageIdInAndUserId(Collection<Long> messageIds, Long userId);
}
