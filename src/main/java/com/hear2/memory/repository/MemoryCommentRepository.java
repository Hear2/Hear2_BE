package com.hear2.memory.repository;

import com.hear2.memory.entity.MemoryComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryCommentRepository extends JpaRepository<MemoryComment, Long> {

    List<MemoryComment> findByCoupleIdAndMemoryIdOrderByCreatedAtAsc(Long coupleId, Long memoryId);

    Optional<MemoryComment> findByIdAndCoupleIdAndMemoryId(Long id, Long coupleId, Long memoryId);

    void deleteByCoupleIdAndMemoryId(Long coupleId, Long memoryId);
}
