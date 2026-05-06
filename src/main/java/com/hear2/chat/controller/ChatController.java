package com.hear2.chat.controller;

import com.hear2.chat.dto.ChatMediaResponse;
import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.service.ChatMediaStorageService;
import com.hear2.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMediaStorageService chatMediaStorageService;

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@RequestBody ChatMessageRequest request) {
        return chatService.saveMessage(request);
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMediaResponse uploadMedia(@RequestParam("file") MultipartFile file) {
        return chatMediaStorageService.store(file);
    }

    @GetMapping("/couples/{coupleId}/messages")
    public List<ChatMessageResponse> getMessages(@PathVariable Long coupleId) {
        return chatService.getMessages(coupleId);
    }
}
