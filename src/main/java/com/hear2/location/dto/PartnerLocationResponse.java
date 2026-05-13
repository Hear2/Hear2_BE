package com.hear2.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "커플 양쪽 위치 조회 응답")
public class PartnerLocationResponse {

    @Schema(description = "내 최신 위치. 내가 아직 위치를 올리지 않았으면 null입니다.")
    private LocationResponse me;

    @Schema(description = "상대방 최신 위치. 상대가 공유를 껐거나 위치가 없으면 null입니다.")
    private LocationResponse partner;

    public static PartnerLocationResponse of(LocationResponse me, LocationResponse partner) {
        return PartnerLocationResponse.builder()
                .me(me)
                .partner(partner)
                .build();
    }
}
