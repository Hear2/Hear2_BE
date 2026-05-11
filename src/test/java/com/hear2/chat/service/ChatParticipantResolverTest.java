package com.hear2.chat.service;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatParticipantResolverTest {

    private final CoupleMemberRepository coupleMemberRepository = mock(CoupleMemberRepository.class);
    private final ChatParticipantResolver resolver = new ChatParticipantResolver(coupleMemberRepository);

    @Test
    void resolvesCoupleAndPartnerFromLoggedInUser() {
        when(coupleMemberRepository.findByUserId(10L))
                .thenReturn(Optional.of(member(1L, 10L)));
        when(coupleMemberRepository.findFirstByCoupleIdAndUserIdNot(1L, 10L))
                .thenReturn(Optional.of(member(1L, 11L)));

        ChatParticipantResolver.ChatRoomContext context = resolver.resolve(10L);

        assertThat(context.coupleId()).isEqualTo(1L);
        assertThat(context.senderId()).isEqualTo(10L);
        assertThat(context.receiverId()).isEqualTo(11L);
    }

    @Test
    void failsWhenCoupleIsMissing() {
        when(coupleMemberRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("couple connection not found");
    }

    private CoupleMember member(Long coupleId, Long userId) {
        return CoupleMember.builder()
                .coupleId(coupleId)
                .userId(userId)
                .role("PARTNER")
                .build();
    }
}
