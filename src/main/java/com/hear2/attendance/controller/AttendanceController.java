package com.hear2.attendance.controller;

import com.hear2.attendance.dto.AttendanceCheckResponse;
import com.hear2.attendance.dto.AttendanceTodayResponse;
import com.hear2.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "오늘 출석체크")
    @PostMapping("/check")
    public AttendanceCheckResponse check(Authentication authentication) {
        return attendanceService.checkToday(currentUserId(authentication));
    }

    @Operation(summary = "오늘 출석 상태 조회")
    @GetMapping("/today")
    public AttendanceTodayResponse today(Authentication authentication) {
        return attendanceService.getToday(currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
