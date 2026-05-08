package com.hear2.emotion.repository;

import com.hear2.emotion.entity.EmotionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmotionAnalysisRepository extends JpaRepository<EmotionAnalysis, Long> {

    Optional<EmotionAnalysis> findByMessageId(Long messageId);

    List<EmotionAnalysis> findByMessageIdIn(Collection<Long> messageIds);
}
