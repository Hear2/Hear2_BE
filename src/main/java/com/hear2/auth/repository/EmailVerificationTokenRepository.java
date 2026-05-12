package com.hear2.auth.repository;

import com.hear2.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByUserId(Long userId);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
