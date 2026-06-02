package com.hear2.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "AI 판사 피드백 저장 요청")
public class JudgeFeedbackRequest {

    @NotNull
    @Schema(description = "판결문 만족 여부. 네=true, 아니요=false", example = "true")
    private Boolean satisfied;

    @Schema(description = "추가 의견. 선택 입력입니다.", example = "상대방 입장을 잘 설명해줘서 도움이 되었어요.", nullable = true)
    private String feedbackText;
}
