package com.hear2.anniversary.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AnniversaryListResponse(
        LocalDate startDate,
        Integer daysTogether,
        List<AnniversaryResponse> anniversaries
) {
}
