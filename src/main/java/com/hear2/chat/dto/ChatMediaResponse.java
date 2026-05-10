package com.hear2.chat.dto;

import com.hear2.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "채팅 미디어 업로드 응답")
public class ChatMediaResponse {

    @Schema(description = "업로드된 파일로 생성할 메시지 타입", example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private MessageType messageType;

    @Schema(description = "채팅 메시지에 첨부할 미디어 URL", example = "/uploads/chat/sample.png")
    private String mediaUrl;

    @Schema(description = "원본 파일명", example = "sample.png")
    private String originalFileName;

    @Schema(description = "미디어 MIME 타입", example = "image/png")
    private String mediaContentType;

    @Schema(description = "미디어 파일 크기(byte)", example = "204800")
    private Long mediaSize;
}
