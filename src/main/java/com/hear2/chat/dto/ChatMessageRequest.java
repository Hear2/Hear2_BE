package com.hear2.chat.dto;

import com.hear2.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    @Schema(description = "커플 ID", example = "1")
    private Long coupleId;

    @Schema(description = "메시지 발신자 ID", example = "10")
    private Long senderId;

    @Schema(description = "메시지 수신자 ID", example = "11")
    private Long receiverId;

    @Schema(description = "메시지 내용. TEXT 메시지는 필수입니다.", example = "오늘 너무 고마웠어")
    private String content;

    @Schema(description = "메시지 타입. 생략하면 TEXT로 처리됩니다.", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "VIDEO"})
    private MessageType messageType;

    @Schema(description = "미디어 파일 URL. IMAGE/VIDEO 메시지에서 사용합니다.", example = "/uploads/chat/sample.png")
    private String mediaUrl;

    @Schema(description = "원본 파일명", example = "sample.png")
    private String originalFileName;

    @Schema(description = "미디어 MIME 타입", example = "image/png")
    private String mediaContentType;

    @Schema(description = "미디어 파일 크기(byte)", example = "204800")
    private Long mediaSize;
}
