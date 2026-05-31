package com.hear2.memory.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.memory.dto.MemoryCalendarResponse;
import com.hear2.memory.dto.MemoryImageTagRequest;
import com.hear2.memory.dto.MemoryImageTagResponse;
import com.hear2.memory.dto.MemoryQuickCreateRequest;
import com.hear2.memory.dto.MemoryQuickResponse;
import com.hear2.memory.dto.MemoryQuickUpdateRequest;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.dto.MemoryYearAgoResponse;
import com.hear2.memory.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Memory Quick", description = "프론트 지침서 기준 3초 기록, 추억 달력 API")
public class MemoryQuickController {

    private final MemoryService memoryService;

    @Operation(
            summary = "3초 기록 생성",
            description = "프론트가 /media/presigned-url로 받은 uploadUrl에 직접 업로드한 뒤 objectKeys 배열과 위치/촬영 시각을 보내 저장합니다. 사진 여러 장은 한 게시물의 photos[]로 묶이며 첫 번째 사진이 커버입니다. userId와 coupleId는 JWT 기준으로 자동 적용됩니다."
    )
    @PostMapping("/api/v1/memory/quick")
    public ApiResponse<MemoryQuickResponse> createQuickMemory(
            Authentication authentication,
            @Valid @RequestBody MemoryQuickCreateRequest request
    ) {
        return ApiResponse.success(memoryService.createQuickMemory(request, currentUserId(authentication)));
    }

    @Operation(
            summary = "3초 기록 수정",
            description = "한 줄 노트와 사용자 태그를 수정합니다. userTags가 null이면 기존 태그 유지, 빈 배열이면 사용자 태그 전체 삭제입니다."
    )
    @PatchMapping("/api/v1/memory/quick/{id}")
    public ApiResponse<MemoryQuickResponse> updateQuickMemory(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MemoryQuickUpdateRequest request
    ) {
        return ApiResponse.success(memoryService.updateQuickMemory(id, request, currentUserId(authentication)));
    }

    @Operation(summary = "추억 달력 조회", description = "로그인 사용자의 커플 기준으로 월별 추억 날짜 요약을 조회합니다.")
    @GetMapping("/api/v1/memory/calendar")
    public ApiResponse<MemoryCalendarResponse> getCalendar(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(memoryService.getCalendar(year, month, currentUserId(authentication)));
    }

    @Operation(summary = "날짜별 추억 조회", description = "로그인 사용자의 커플 기준으로 특정 날짜의 추억을 조회합니다.")
    @GetMapping("/api/v1/memory/by-date")
    public ApiResponse<List<MemoryResponse>> getMemoriesByDate(
            Authentication authentication,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate date
    ) {
        return ApiResponse.success(memoryService.getMemoriesByDate(date, currentUserId(authentication)));
    }

    @Operation(summary = "1년 전 오늘 추억 조회", description = "로그인 사용자의 커플 기준으로 1년 전 오늘의 추억을 조회합니다.")
    @GetMapping("/api/v1/memory/year-ago")
    public ApiResponse<MemoryYearAgoResponse> getYearAgo(Authentication authentication) {
        return ApiResponse.success(memoryService.getYearAgo(currentUserId(authentication)));
    }

    @Operation(summary = "이미지 AI 태그 수동 생성", description = "이미지 URL을 기준으로 AI 태그를 생성합니다. 업로드 시 자동 태깅과 같은 분석 서비스를 사용합니다.")
    @PostMapping("/api/v1/ai/image-tags")
    public ApiResponse<MemoryImageTagResponse> analyzeImageTags(
            Authentication authentication,
            @Valid @RequestBody MemoryImageTagRequest request
    ) {
        return ApiResponse.success(memoryService.analyzeImageTags(request, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
