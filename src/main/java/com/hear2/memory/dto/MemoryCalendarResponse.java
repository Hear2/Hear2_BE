package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "월별 추억 달력 응답")
public class MemoryCalendarResponse {

    private int year;
    private int month;
    private List<MemoryCalendarDayResponse> days;
}
