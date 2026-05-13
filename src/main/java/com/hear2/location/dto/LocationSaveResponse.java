package com.hear2.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "내 위치 저장 응답")
public class LocationSaveResponse {

    @Schema(description = "저장 성공 여부", example = "true")
    private boolean ok;

    @Schema(description = "서버 저장 시각")
    private LocalDateTime savedAt;

    public static LocationSaveResponse saved(LocalDateTime savedAt) {
        return LocationSaveResponse.builder()
                .ok(true)
                .savedAt(savedAt)
                .build();
    }
}
