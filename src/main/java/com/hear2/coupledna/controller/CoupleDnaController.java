package com.hear2.coupledna.controller;

import com.hear2.coupledna.dto.CoupleDnaResponse;
import com.hear2.coupledna.service.CoupleDnaService;
import com.hear2.global.error.ApiErrorResponse;
import com.hear2.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Couple DNA", description = "커플 채팅/감정 데이터를 기반으로 한 Couple DNA 분석 API")
public class CoupleDnaController {

    private final CoupleDnaService coupleDnaService;

    @Operation(summary = "Couple DNA 조회", description = "최근 대화/감정/갈등 데이터를 기반으로 커플의 소통 DNA를 계산해 JSON으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Couple DNA 조회 성공",
                    content = @Content(schema = @Schema(implementation = CoupleDnaResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/api/v1/couples/{coupleId}/dna")
    public ApiResponse<CoupleDnaResponse> getCoupleDna(
            @Parameter(description = "커플 ID", example = "1", required = true)
            @PathVariable Long coupleId,
            @Parameter(description = "분석 기준 날짜. 미입력 시 오늘 기준 최근 120일", example = "2026-05-12")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate anchorDate
    ) {
        return ApiResponse.success(coupleDnaService.getCoupleDna(coupleId, anchorDate));
    }
}
