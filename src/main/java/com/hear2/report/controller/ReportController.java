package com.hear2.report.controller;

import com.hear2.global.error.ApiErrorResponse;
import com.hear2.global.response.ApiResponse;
import com.hear2.report.dto.ReportResponse;
import com.hear2.report.dto.ReportShareCreateRequest;
import com.hear2.report.dto.ReportShareResponse;
import com.hear2.report.service.ReportService;
import com.hear2.report.service.ReportShareService;
import com.hear2.report.support.ReportType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI Report", description = "주간/월간 감정 리포트 조회 및 공유 API")
public class ReportController {

    private final ReportService reportService;
    private final ReportShareService reportShareService;

    @Operation(summary = "AI 리포트 조회", description = "커플의 대화/감정 분석/AI 판사 이력을 기반으로 주간 또는 월간 리포트를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/api/v1/reports/couples/{coupleId}")
    public ApiResponse<ReportResponse> getReport(
            @Parameter(description = "커플 ID", example = "1", required = true)
            @PathVariable Long coupleId,
            @Parameter(description = "리포트 타입", example = "WEEKLY")
            @RequestParam(defaultValue = "WEEKLY") ReportType reportType,
            @Parameter(description = "기준 날짜", example = "2026-05-07")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate anchorDate
    ) {
        return ApiResponse.success(reportService.getReport(coupleId, reportType, anchorDate));
    }

    @Operation(summary = "AI 리포트 공유 링크 생성", description = "현재 리포트 스냅샷을 저장하고 외부/상대방에게 전달할 공유 링크를 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 링크 생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/api/v1/reports/shares")
    public ApiResponse<ReportShareResponse> createShare(@RequestBody ReportShareCreateRequest request) {
        return ApiResponse.success(reportShareService.createShare(request));
    }

    @Operation(summary = "공유된 AI 리포트 JSON 조회", description = "공유 코드 기준으로 저장된 리포트 스냅샷 JSON을 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 리포트 조회 성공",
            content = @Content(schema = @Schema(implementation = ReportResponse.class)))
    @GetMapping("/api/v1/reports/shares/{shareCode}")
    public ApiResponse<ReportResponse> getSharedReport(
            @Parameter(description = "공유 코드", example = "7ce8db13b4b2", required = true)
            @PathVariable String shareCode
    ) {
        return ApiResponse.success(reportShareService.getSharedReport(shareCode));
    }

    @Operation(summary = "공유된 AI 리포트 공개 JSON 조회", description = "공유 코드 기준으로 공개 접근 가능한 리포트 스냅샷 JSON을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 리포트 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 코드 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/reports/shared/{shareCode}")
    public ApiResponse<ReportResponse> getPublicSharedReport(
            @Parameter(description = "공유 코드", example = "7ce8db13b4b2", required = true)
            @PathVariable String shareCode
    ) {
        return ApiResponse.success(reportShareService.getSharedReport(shareCode));
    }
}
