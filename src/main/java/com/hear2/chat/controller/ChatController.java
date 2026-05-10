package com.hear2.chat.controller;

import com.hear2.chat.dto.ChatMediaResponse;
import com.hear2.chat.dto.ChatMessageRequest;
import com.hear2.chat.dto.ChatMessageResponse;
import com.hear2.chat.service.ChatMediaStorageService;
import com.hear2.chat.service.ChatService;
import com.hear2.global.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 메시지 전송, 조회, 미디어 업로드 API")
public class ChatController {

    private final ChatService chatService;
    private final ChatMediaStorageService chatMediaStorageService;

    @Operation(
            summary = "채팅 메시지 전송",
            description = "TEXT 메시지는 저장 직후 감정 분석을 수행하고, 감정 이모지와 리스크 정보를 응답에 포함합니다. 리스크가 감지되면 상대방에게 FCM 알림 발송을 시도합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 전송 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@RequestBody ChatMessageRequest request) {
        return chatService.saveMessage(request);
    }

    @Operation(summary = "채팅 미디어 업로드", description = "이미지 또는 동영상 파일을 업로드하고 채팅 메시지에서 사용할 미디어 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "미디어 업로드 성공",
                    content = @Content(schema = @Schema(implementation = ChatMediaResponse.class))),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식 또는 요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMediaResponse uploadMedia(
            @Parameter(description = "업로드할 이미지 또는 동영상 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return chatMediaStorageService.store(file);
    }

    @Operation(summary = "커플 채팅 메시지 목록 조회", description = "커플 ID 기준으로 채팅 메시지를 오래된 순서로 조회하며, 저장된 감정 분석 결과를 함께 반환합니다.")
    @ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공")
    @GetMapping("/couples/{coupleId}/messages")
    public List<ChatMessageResponse> getMessages(
            @Parameter(description = "커플 ID", example = "1", required = true)
            @PathVariable Long coupleId
    ) {
        return chatService.getMessages(coupleId);
    }
}
