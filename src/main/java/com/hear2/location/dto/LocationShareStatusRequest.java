package com.hear2.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "위치 공유 ON/OFF 요청. userId와 coupleId는 JWT 기준으로 자동 적용됩니다.",
        example = """
                {
                  "enabled": true
                }
                """
)
public class LocationShareStatusRequest {

    @NotNull
    @Schema(description = "위치 공유 활성화 여부", example = "true")
    private Boolean enabled;
}
