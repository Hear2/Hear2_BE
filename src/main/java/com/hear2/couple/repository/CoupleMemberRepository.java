package com.hear2.couple.repository;

import com.hear2.couple.entity.CoupleMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoupleMemberRepository extends JpaRepository<CoupleMember, Long> {

    Optional<CoupleMember> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByCoupleIdAndUserId(Long coupleId, Long userId);

    long countByCoupleId(Long coupleId);
}
