package com.hear2.memory.repository;

import com.hear2.memory.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByCoupleId(Long coupleId);

    List<Memory> findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
            Long coupleId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Memory> findByIdAndCoupleId(Long id, Long coupleId);

    @Query("""
            select count(distinct m.id)
            from Memory m
            left join m.photos p
            where m.coupleId = :coupleId
              and m.id <> :excludedMemoryId
              and (m.storedPhotoPath = :storedPhotoPath or p.storedPhotoPath = :storedPhotoPath)
            """)
    long countOtherReferencesByCoupleIdAndStoredPhotoPath(
            @Param("coupleId") Long coupleId,
            @Param("excludedMemoryId") Long excludedMemoryId,
            @Param("storedPhotoPath") String storedPhotoPath
    );
}
