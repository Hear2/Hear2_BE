package com.hear2.capsule.repository;

import com.hear2.capsule.entity.TimeCapsuleLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeCapsuleLetterRepository extends JpaRepository<TimeCapsuleLetter, Long> {

    Optional<TimeCapsuleLetter> findByCapsuleId(Long capsuleId);
}
