package com.hear2.anniversary.repository;

import com.hear2.anniversary.entity.HiddenAutoAnniversary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HiddenAutoAnniversaryRepository extends JpaRepository<HiddenAutoAnniversary, Long> {

    List<HiddenAutoAnniversary> findByCoupleIdAndUserId(Long coupleId, Long userId);

    Optional<HiddenAutoAnniversary> findByCoupleIdAndUserIdAndAutoKey(
            Long coupleId,
            Long userId,
            String autoKey
    );

    void deleteByCoupleIdAndUserIdAndAutoKey(
            Long coupleId,
            Long userId,
            String autoKey
    );

    void deleteByCoupleIdAndUserIdAndAutoKeyIn(
            Long coupleId,
            Long userId,
            Collection<String> autoKeys
    );
}
