package com.hear2.capsule.dto;

import com.hear2.capsule.entity.TimeCapsuleCoverStyle;
import com.hear2.capsule.entity.TimeCapsuleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TimeCapsuleDetailResponse {

    @Schema(description = "타임캡슐 ID", example = "1")
    private Long id;

    @Schema(description = "타임캡슐 이름", example = "1주년 기념 캡슐")
    private String name;

    @Schema(description = "타임캡슐 상태. SEALED이면 편지/사진/signed URL은 숨겨집니다.", example = "OPEN")
    private TimeCapsuleStatus status;

    @Schema(description = "커버 스타일", example = "LETTER")
    private TimeCapsuleCoverStyle coverStyle;

    @Schema(description = "봉인 시각. UTC 기준으로 저장됩니다.")
    private LocalDateTime sealedAt;

    @Schema(description = "개봉 예정 시각. UTC 기준으로 저장됩니다.")
    private LocalDateTime openAt;

    @Schema(description = "실제 개봉 처리 시각. sealed 상태면 null", nullable = true)
    private LocalDateTime openedAt;

    @Schema(description = "편지 정보. sealed 상태면 null", nullable = true)
    private TimeCapsuleLetterResponse letter;

    @Schema(description = "사진 정보. sealed 상태면 빈 배열")
    private List<TimeCapsulePhotoResponse> photos;

    @Schema(description = "봉인 시점과 현재의 비교 데이터. sealed 상태면 null", nullable = true)
    private TimeCapsuleThenVsNowResponse thenVsNow;

    @Schema(description = "인스타 스토리 공유용 카드 데이터. sealed 상태면 null", nullable = true)
    private TimeCapsuleShareCardResponse shareCard;
}
