package com.hear2.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        description = "미디어 직접 업로드용 presigned URL 발급 요청",
        example = """
                {
                  "mediaType": "photo",
                  "contentType": "image/jpeg",
                  "originalFileName": "memory.jpg",
                  "purpose": "memory"
                }
                """
)
public class MediaPresignedUrlRequest {

    @NotBlank
    @Pattern(regexp = "photo|voice")
    @Schema(description = "미디어 타입", allowableValues = {"photo", "voice"}, example = "photo")
    private String mediaType;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "업로드할 파일의 Content-Type", example = "image/jpeg")
    private String contentType;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "원본 파일명", example = "memory.jpg")
    private String originalFileName;

    @Size(max = 40)
    @Schema(description = "저장 목적. memory, chat 등", example = "memory")
    private String purpose;
}
