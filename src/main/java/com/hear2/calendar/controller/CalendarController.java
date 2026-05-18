package com.hear2.calendar.controller;

import com.hear2.calendar.dto.CalendarDateDetailResponse;
import com.hear2.calendar.dto.CalendarEventCreateRequest;
import com.hear2.calendar.dto.CalendarEventResponse;
import com.hear2.calendar.dto.CalendarEventUpdateRequest;
import com.hear2.calendar.dto.CalendarMonthResponse;
import com.hear2.calendar.dto.CalendarUpcomingResponse;
import com.hear2.calendar.service.CalendarService;
import com.hear2.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Shared Calendar", description = "공유 캘린더와 추억 달력 통합 API")
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(
            summary = "일정 생성",
            description = """
                    로그인 사용자의 커플 기준으로 일정을 생성합니다.
                    userId/coupleId는 JWT로 자동 적용되므로 Swagger에서는 Authorize 후 본문만 입력하면 됩니다.
                    target이 MINE이면 내 일정, PARTNER이면 상대 일정, SHARED이면 공동 일정으로 저장됩니다.
                    Google/iOS/Android 캘린더 연동은 후속 단계이며, 현재 응답에는 추후 연동용 외부 캘린더 필드가 포함됩니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CalendarEventCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "공동 일정",
                                    value = """
                                            {
                                              "title": "서울숲 데이트",
                                              "target": "SHARED",
                                              "startsAt": "2026-04-12T05:00:00Z",
                                              "endsAt": "2026-04-12T10:00:00Z",
                                              "allDay": false,
                                              "locationName": "서울숲",
                                              "addressName": "서울시 성동구 서울숲길 273",
                                              "latitude": 37.5444,
                                              "longitude": 127.0374,
                                              "memo": "벚꽃 보고 카페 들르기",
                                              "tags": ["데이트", "봄"],
                                              "remindBeforeMinutes": 30,
                                              "memoryIds": []
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/api/v1/calendar/events")
    public ApiResponse<CalendarEventResponse> createEvent(
            Authentication authentication,
            @Valid @RequestBody CalendarEventCreateRequest request
    ) {
        return ApiResponse.success(calendarService.createEvent(request, currentUserId(authentication)));
    }

    @Operation(summary = "캘린더 월별 통합 조회", description = "월별 일정과 추억 ❤️ 마커를 함께 조회합니다.")
    @GetMapping("/api/v1/calendar/month")
    public ApiResponse<CalendarMonthResponse> getMonth(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(calendarService.getMonth(year, month, currentUserId(authentication)));
    }

    @Operation(summary = "날짜 상세 조회", description = "특정 날짜의 일정 목록과 해당 날짜 추억 목록을 함께 조회합니다.")
    @GetMapping("/api/v1/calendar/dates/{date}")
    public ApiResponse<CalendarDateDetailResponse> getDateDetail(
            Authentication authentication,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PathVariable LocalDate date
    ) {
        return ApiResponse.success(calendarService.getDateDetail(date, currentUserId(authentication)));
    }

    @Operation(summary = "다가오는 일정 조회", description = "로그인 사용자의 커플 기준으로 현재 이후 일정을 가까운 순서로 조회합니다.")
    @GetMapping("/api/v1/calendar/upcoming")
    public ApiResponse<CalendarUpcomingResponse> getUpcoming(
            Authentication authentication,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(calendarService.getUpcoming(limit, currentUserId(authentication)));
    }

    @Operation(summary = "일정 상세 조회", description = "일정 상세와 연결된 추억을 조회합니다.")
    @GetMapping("/api/v1/calendar/events/{eventId}")
    public ApiResponse<CalendarEventResponse> getEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        return ApiResponse.success(calendarService.getEvent(eventId, currentUserId(authentication)));
    }

    @Operation(summary = "일정 수정", description = "일정을 전체 교체 방식으로 수정합니다.")
    @PatchMapping("/api/v1/calendar/events/{eventId}")
    public ApiResponse<CalendarEventResponse> updateEvent(
            Authentication authentication,
            @PathVariable Long eventId,
            @Valid @RequestBody CalendarEventUpdateRequest request
    ) {
        return ApiResponse.success(calendarService.updateEvent(eventId, request, currentUserId(authentication)));
    }

    @Operation(summary = "일정 삭제", description = "일정과 일정-추억 연결을 삭제합니다. 연결된 추억 자체는 삭제하지 않습니다.")
    @DeleteMapping("/api/v1/calendar/events/{eventId}")
    public ApiResponse<Void> deleteEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        calendarService.deleteEvent(eventId, currentUserId(authentication));
        return ApiResponse.success(null, "calendar event deleted");
    }

    @Operation(summary = "일정에 추억 연결", description = "일정 상세에서 보여줄 추억을 연결합니다.")
    @PostMapping("/api/v1/calendar/events/{eventId}/memories/{memoryId}")
    public ApiResponse<CalendarEventResponse> linkMemory(
            Authentication authentication,
            @PathVariable Long eventId,
            @PathVariable Long memoryId
    ) {
        return ApiResponse.success(calendarService.linkMemory(eventId, memoryId, currentUserId(authentication)));
    }

    @Operation(summary = "일정에서 추억 연결 해제", description = "일정과 추억의 연결만 해제합니다.")
    @DeleteMapping("/api/v1/calendar/events/{eventId}/memories/{memoryId}")
    public ApiResponse<CalendarEventResponse> unlinkMemory(
            Authentication authentication,
            @PathVariable Long eventId,
            @PathVariable Long memoryId
    ) {
        return ApiResponse.success(calendarService.unlinkMemory(eventId, memoryId, currentUserId(authentication)));
    }

    @Operation(summary = "추억에 연결된 일정 조회", description = "추억 상세 화면에서 연결된 일정을 보여줄 때 사용합니다.")
    @GetMapping("/api/v1/calendar/memories/{memoryId}/events")
    public ApiResponse<List<CalendarEventResponse>> getEventsByMemory(
            Authentication authentication,
            @PathVariable Long memoryId
    ) {
        return ApiResponse.success(calendarService.getEventsByMemory(memoryId, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
