package com.hear2.capsule.repository;

import com.hear2.capsule.entity.TimeCapsuleSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeCapsuleSnapshotRepository extends JpaRepository<TimeCapsuleSnapshot, Long> {

    Optional<TimeCapsuleSnapshot> findByCapsuleId(Long capsuleId);
}
