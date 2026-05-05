package com.hear2.chat.controller;

import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.dto.ChatReadRequest;
import com.hear2.chat.dto.ChatReadResponse;
import com.hear2.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/messages")
    public void sendMessage(ChatMessageRequest request) {
        ChatMessageResponse response = chatService.saveMessage(request);

        messagingTemplate.convertAndSend(
                "/sub/chats/couples/" + response.getCoupleId(),
                response
        );
    }

    @MessageMapping("/chats/read")
    public void readMessages(ChatReadRequest request) {
        ChatReadResponse response = chatService.markMessagesAsRead(request);

        if (response.getReadMessageIds().isEmpty()) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/sub/chats/couples/" + response.getCoupleId() + "/read",
                response
        );
    }
}
