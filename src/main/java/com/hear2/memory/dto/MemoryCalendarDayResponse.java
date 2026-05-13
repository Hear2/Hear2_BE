package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "추억 달력 날짜별 요약")
public class MemoryCalendarDayResponse {

    private LocalDate date;
    private List<String> thumbnails;
    private int memoryCount;
    private String dominantEmoji;
}
