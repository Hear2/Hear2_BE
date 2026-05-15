package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TimeCapsuleLetterResponse {

    @Schema(description = "편지 작성자 userId", example = "17")
    private Long authorId;

    @Schema(description = "편지 작성자 이름. 현재는 null이며 사용자 프로필 연동 시 채워집니다.", nullable = true)
    private String authorName;

    @Schema(description = "편지 내용", example = "지금 이 순간이 너무 행복해 사랑해")
    private String text;

    @Schema(description = "편지 작성 시각. UTC 기준으로 저장됩니다.")
    private LocalDateTime writtenAt;
}
