package com.hear2.chat.repository;

import com.hear2.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByCoupleIdOrderByCreatedAtAsc(Long coupleId);

    List<ChatMessage> findByCoupleIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            Long coupleId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    List<ChatMessage> findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(Long coupleId, Long receiverId);

    List<ChatMessage> findTop20ByCoupleIdOrderByCreatedAtDesc(
            Long coupleId
    );

    Optional<ChatMessage> findByIdAndCoupleId(Long id, Long coupleId);

    long countByCoupleId(Long coupleId);
}
