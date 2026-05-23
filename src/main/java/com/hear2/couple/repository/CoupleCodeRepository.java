package com.hear2.couple.repository;

import com.hear2.couple.entity.CoupleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoupleCodeRepository extends JpaRepository<CoupleCode, Long> {

    Optional<CoupleCode> findByCode(String code);

    boolean existsByCode(String code);

    List<CoupleCode> findByIssuerUserIdAndUsedAtIsNull(Long issuerUserId);

    Optional<CoupleCode> findTopByIssuerUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long issuerUserId);
}
