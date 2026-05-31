package com.hear2.calendar.dto;

import com.hear2.memory.dto.MemoryResponse;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record CalendarDateDetailResponse(
        LocalDate date,
        List<CalendarEventResponse> events,
        List<MemoryResponse> memories
) {
}
