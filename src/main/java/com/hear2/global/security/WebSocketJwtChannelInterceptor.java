package com.hear2.global.security;

import com.hear2.chat.service.ChatParticipantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHAT_SUBSCRIBE_PREFIX = "/sub/chats/couples/";
    private static final String CHAT_PUBLISH_PREFIX = "/pub/chats/";
    private static final String LOCATION_SUBSCRIBE_PREFIX = "/sub/locations/couples/";
    private static final String LOCATION_PUBLISH_PREFIX = "/pub/locations/";

    private final JwtProvider jwtProvider;
    private final ChatParticipantResolver chatParticipantResolver;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SEND
                && (isChatPublish(accessor.getDestination()) || isLocationPublish(accessor.getDestination()))) {
            requireUser(accessor);
            return message;
        }

        if (command == StompCommand.SUBSCRIBE
                && (isChatSubscribe(accessor.getDestination()) || isLocationSubscribe(accessor.getDestination()))) {
            Long userId = requireUser(accessor);
            validateCoupleSubscription(userId, accessor.getDestination());
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new AccessDeniedException("login is required");
        }

        Long userId = jwtProvider.getUserId(token);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private Long requireUser(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }

        throw new AccessDeniedException("login is required");
    }

    private void validateCoupleSubscription(Long userId, String destination) {
        Long subscribedCoupleId = extractCoupleId(destination);
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(userId);
        if (!context.coupleId().equals(subscribedCoupleId)) {
            throw new AccessDeniedException(resolveAccessDeniedMessage(destination));
        }
    }

    private boolean isChatPublish(String destination) {
        return StringUtils.hasText(destination) && destination.startsWith(CHAT_PUBLISH_PREFIX);
    }

    private boolean isChatSubscribe(String destination) {
        return StringUtils.hasText(destination) && destination.startsWith(CHAT_SUBSCRIBE_PREFIX);
    }

    private boolean isLocationPublish(String destination) {
        return StringUtils.hasText(destination) && destination.startsWith(LOCATION_PUBLISH_PREFIX);
    }

    private boolean isLocationSubscribe(String destination) {
        return StringUtils.hasText(destination) && destination.startsWith(LOCATION_SUBSCRIBE_PREFIX);
    }

    private Long extractCoupleId(String destination) {
        String prefix = destination.startsWith(CHAT_SUBSCRIBE_PREFIX)
                ? CHAT_SUBSCRIBE_PREFIX
                : LOCATION_SUBSCRIBE_PREFIX;
        String suffix = destination.substring(prefix.length());
        int slashIndex = suffix.indexOf('/');
        String coupleId = slashIndex < 0 ? suffix : suffix.substring(0, slashIndex);
        try {
            return Long.valueOf(coupleId);
        } catch (NumberFormatException exception) {
            throw new MessageDeliveryException("invalid couple subscription destination");
        }
    }

    private String resolveAccessDeniedMessage(String destination) {
        if (destination.startsWith(CHAT_SUBSCRIBE_PREFIX)) {
            return "chat room access denied";
        }

        return "location channel access denied";
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length());
    }
}
