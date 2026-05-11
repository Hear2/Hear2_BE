package com.hear2.couple.repository;

import com.hear2.couple.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

    Optional<Couple> findByCoupleCode(String coupleCode);

    boolean existsByCoupleCode(String coupleCode);
}
