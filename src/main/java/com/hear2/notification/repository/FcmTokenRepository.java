package com.hear2.notification.repository;

import com.hear2.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUserIdAndActiveTrue(Long userId);
}
