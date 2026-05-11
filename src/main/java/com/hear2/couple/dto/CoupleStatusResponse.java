package com.hear2.couple.dto;

import com.hear2.couple.entity.Couple;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CoupleStatusResponse {

    private boolean connected;

    private Long coupleId;

    private String coupleCode;

    private LocalDate startDate;

    private LocalDateTime createdAt;

    private long memberCount;

    public static CoupleStatusResponse disconnected() {
        return CoupleStatusResponse.builder()
                .connected(false)
                .memberCount(0)
                .build();
    }

    public static CoupleStatusResponse from(Couple couple, long memberCount) {
        return CoupleStatusResponse.builder()
                .connected(memberCount >= 2)
                .coupleId(couple.getCoupleId())
                .coupleCode(couple.getCoupleCode())
                .startDate(couple.getStartDate())
                .createdAt(couple.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
