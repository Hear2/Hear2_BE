package com.hear2.memory.repository;

import com.hear2.memory.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByCoupleIdOrderByMemoryDateDescCreatedAtDesc(Long coupleId);

    List<Memory> findByCoupleIdAndMemoryDateOrderByCreatedAtDesc(Long coupleId, LocalDate memoryDate);

    long countByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqual(
            Long coupleId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Memory> findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
            Long coupleId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Memory> findByIdAndCoupleId(Long id, Long coupleId);
}
