package com.hear2.chat.repository;

import com.hear2.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByCoupleIdOrderByCreatedAtAsc(Long coupleId);

    List<ChatMessage> findByCoupleIdAndReceiverIdAndReadAtIsNullOrderByCreatedAtAsc(Long coupleId, Long receiverId);

    List<ChatMessage> findTop20ByCoupleIdOrderByCreatedAtDesc(
            Long coupleId
    );
}
