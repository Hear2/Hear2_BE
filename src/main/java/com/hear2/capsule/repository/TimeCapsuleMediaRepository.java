package com.hear2.capsule.repository;

import com.hear2.capsule.entity.TimeCapsuleMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeCapsuleMediaRepository extends JpaRepository<TimeCapsuleMedia, Long> {

    List<TimeCapsuleMedia> findByCapsuleIdOrderByOrderIndexAsc(Long capsuleId);
}
