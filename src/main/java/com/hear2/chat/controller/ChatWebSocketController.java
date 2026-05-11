package com.hear2.chat.controller;

import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.dto.ChatReadRequest;
import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/messages")
    public void sendMessage(Authentication authentication, ChatMessageRequest request) {
        ChatMessageResponse response = chatService.saveMessage(currentUserId(authentication), request);

        messagingTemplate.convertAndSend(
                "/sub/chats/couples/" + response.getCoupleId(),
                response
        );
    }

    @MessageMapping("/chats/read")
    public void readMessages(Authentication authentication, ChatReadRequest request) {
        ChatReadResponse response = chatService.markMessagesAsRead(currentUserId(authentication));

        if (response.getReadMessageIds().isEmpty()) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/sub/chats/couples/" + response.getCoupleId() + "/read",
                response
        );
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
