package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(
        description = "3초 기록 수정 요청",
        example = """
                {
                  "note": "오늘도 행복한 하루",
                  "userTags": ["우리둘이"]
                }
                """
)
public class MemoryQuickUpdateRequest {

    @Size(max = 1000)
    @Schema(description = "한 줄 노트", example = "오늘도 행복한 하루")
    private String note;

    @Size(max = 20)
    @Schema(description = "사용자 직접 입력 태그. null이면 기존 태그를 유지하고, []이면 모두 삭제합니다.", example = "[\"우리둘이\"]")
    private List<@Size(max = 80) String> userTags;
}
