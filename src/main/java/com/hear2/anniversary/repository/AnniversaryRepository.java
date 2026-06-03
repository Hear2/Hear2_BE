package com.hear2.anniversary.repository;

import com.hear2.anniversary.entity.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {

    Optional<Anniversary> findByIdAndCoupleId(Long id, Long coupleId);

    List<Anniversary> findByCoupleIdOrderByAnniversaryDateAscIdAsc(Long coupleId);

    List<Anniversary> findByCoupleIdAndSharedTrueOrderByAnniversaryDateAscIdAsc(Long coupleId);

    List<Anniversary> findByCoupleIdAndCreatedByOrderByAnniversaryDateAscIdAsc(Long coupleId, Long createdBy);
}
