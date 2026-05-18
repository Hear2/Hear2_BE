package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        description = "추억 우리의 한마디 작성 요청",
        example = """
                {
                  "content": "이날 같이 가자고 한 거 진짜 잘했다"
                }
                """
)
public class MemoryCommentCreateRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "우리의 한마디 내용", example = "이날 같이 가자고 한 거 진짜 잘했다")
    private String content;
}
