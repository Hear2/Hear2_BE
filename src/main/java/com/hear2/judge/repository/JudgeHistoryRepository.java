package com.hear2.judge.repository;

import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JudgeHistoryRepository extends JpaRepository<JudgeHistory, Long> {

    List<JudgeHistory> findByCoupleIdOrderByCreatedAtDesc(Long coupleId);

    Long countByCoupleIdAndConflictType(Long coupleId, ConflictType conflictType);
}
