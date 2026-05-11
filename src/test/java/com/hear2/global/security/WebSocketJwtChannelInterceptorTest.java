package com.hear2.global.security;

import com.hear2.chat.service.ChatParticipantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketJwtChannelInterceptorTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-secret-key-for-websocket-auth",
            3600L
    );
    private final ChatParticipantResolver chatParticipantResolver = mock(ChatParticipantResolver.class);
    private final WebSocketJwtChannelInterceptor interceptor = new WebSocketJwtChannelInterceptor(
            jwtProvider,
            chatParticipantResolver
    );
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void rejectsConnectWithoutBearerToken() {
        Message<byte[]> message = message(StompCommand.CONNECT, null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("login is required");
    }

    @Test
    void allowsConnectWithBearerToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(10L));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> interceptor.preSend(message, channel))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsChatSubscriptionForUsersCouple() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        Message<byte[]> message = message(
                StompCommand.SUBSCRIBE,
                "/sub/chats/couples/1",
                new UsernamePasswordAuthenticationToken(10L, null, List.of())
        );

        interceptor.preSend(message, channel);
    }

    @Test
    void rejectsChatSubscriptionForAnotherCouple() {
        when(chatParticipantResolver.resolve(10L))
                .thenReturn(new ChatParticipantResolver.ChatRoomContext(1L, 10L, 11L));
        Message<byte[]> message = message(
                StompCommand.SUBSCRIBE,
                "/sub/chats/couples/2",
                new UsernamePasswordAuthenticationToken(10L, null, List.of())
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chat room access denied");
    }

    @Test
    void rejectsChatPublishWithoutAuthenticatedSession() {
        Message<byte[]> message = message(StompCommand.SEND, "/pub/chats/messages", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("login is required");
    }

    private Message<byte[]> message(StompCommand command, String destination, UsernamePasswordAuthenticationToken user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
