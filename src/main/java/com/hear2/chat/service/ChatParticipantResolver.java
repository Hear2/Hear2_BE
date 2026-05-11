package com.hear2.chat.service;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ChatParticipantResolver {

    private final CoupleMemberRepository coupleMemberRepository;

    public ChatRoomContext resolve(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }

        CoupleMember member = coupleMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));

        CoupleMember partner = coupleMemberRepository.findFirstByCoupleIdAndUserIdNot(member.getCoupleId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "partner not connected yet"));

        return new ChatRoomContext(member.getCoupleId(), userId, partner.getUserId());
    }

    public record ChatRoomContext(Long coupleId, Long senderId, Long receiverId) {
    }
}
