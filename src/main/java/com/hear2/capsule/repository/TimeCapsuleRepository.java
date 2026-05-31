package com.hear2.capsule.repository;

import com.hear2.capsule.entity.TimeCapsule;
import com.hear2.capsule.entity.TimeCapsuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeCapsuleRepository extends JpaRepository<TimeCapsule, Long> {

    List<TimeCapsule> findByCoupleIdOrderByOpenAtDesc(Long coupleId);

    List<TimeCapsule> findByCoupleIdAndStatusOrderByOpenAtDesc(Long coupleId, TimeCapsuleStatus status);

    List<TimeCapsule> findByStatusAndOpenAtLessThanEqual(TimeCapsuleStatus status, LocalDateTime openAt);

    Optional<TimeCapsule> findByIdAndCoupleId(Long id, Long coupleId);
}
