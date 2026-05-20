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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
            description = "로그인된 사용자의 커플 정보를 기준으로 메시지를 저장합니다. TEXT 메시지는 먼저 저장되고, 감정 분석은 커밋 이후 비동기로 수행됩니다. 따라서 POST 응답에서는 emotionType/emotionScore/emotionEmoji/riskLevel/judgeAvailable 등의 감정 분석 필드가 null 또는 기본값일 수 있습니다. AI 서버 장애가 있어도 메시지 저장은 성공해야 하며, 감정 분석이 나중에 성공하면 이후 목록 재조회 시 반영됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 전송 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "로그인된 사용자의 커플 정보가 자동 적용됩니다. TEXT 메시지는 content/messageType만 보내면 됩니다.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ChatMessageRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "TEXT 메시지",
                                    summary = "텍스트 전송",
                                    value = """
                                            {
                                              "content": "로그인 테스트",
                                              "messageType": "TEXT"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "IMAGE 메시지",
                                    summary = "업로드한 이미지 전송",
                                    value = """
                                            {
                                              "content": "",
                                              "messageType": "IMAGE",
                                              "mediaUrl": "/uploads/chat/sample.png",
                                              "originalFileName": "sample.png",
                                              "mediaContentType": "image/png",
                                              "mediaSize": 204800
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(Authentication authentication, @RequestBody ChatMessageRequest request) {
        return chatService.saveMessage(currentUserId(authentication), request);
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

    @Operation(summary = "내 채팅 메시지 목록 조회", description = "로그인된 사용자의 커플 기준으로 채팅 메시지를 오래된 순서로 조회합니다. 이미 emotion_analysis에 저장된 감정 분석 결과가 있으면 emotionType/emotionEmoji/riskLevel/judgeAvailable 등을 함께 반환하고, 아직 분석 전이거나 분석 실패한 메시지는 해당 감정 필드가 null 또는 기본값으로 반환됩니다.")
    @ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공")
    @GetMapping("/messages")
    public List<ChatMessageResponse> getMessages(Authentication authentication) {
        return chatService.getMessages(currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
