package com.hear2.calendar.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CalendarPlaceSearchResultResponse(
        String id,
        String placeName,
        String addressName,
        String roadAddressName,
        BigDecimal latitude,
        BigDecimal longitude,
        String categoryName,
        String phone,
        String placeUrl
) {
}
