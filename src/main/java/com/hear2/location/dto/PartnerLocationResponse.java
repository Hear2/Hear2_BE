package com.hear2.location.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PartnerLocationResponse {

    private Long coupleId;
    private Long requesterId;
    private boolean shared;
    private LocationResponse location;

    public static PartnerLocationResponse shared(Long coupleId, Long requesterId, LocationResponse location) {
        return PartnerLocationResponse.builder()
                .coupleId(coupleId)
                .requesterId(requesterId)
                .shared(true)
                .location(location)
                .build();
    }

    public static PartnerLocationResponse hidden(Long coupleId, Long requesterId) {
        return PartnerLocationResponse.builder()
                .coupleId(coupleId)
                .requesterId(requesterId)
                .shared(false)
                .location(null)
                .build();
    }
}
