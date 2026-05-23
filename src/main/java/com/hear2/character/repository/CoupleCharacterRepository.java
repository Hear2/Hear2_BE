package com.hear2.character.repository;

import com.hear2.character.entity.CoupleCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoupleCharacterRepository extends JpaRepository<CoupleCharacter, Long> {

    Optional<CoupleCharacter> findByCoupleId(Long coupleId);

    boolean existsByCoupleId(Long coupleId);
}
