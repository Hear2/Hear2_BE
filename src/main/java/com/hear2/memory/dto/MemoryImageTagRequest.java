package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        description = "이미지 AI 태그 수동 생성 요청",
        example = """
                {
                  "imageUrl": "https://cdn.example.com/memories/2/photo.jpg"
                }
                """
)
public class MemoryImageTagRequest {

    @NotBlank
    @Size(max = 2048)
    @Schema(description = "분석할 이미지 URL", example = "https://cdn.example.com/memories/2/photo.jpg")
    private String imageUrl;
}
