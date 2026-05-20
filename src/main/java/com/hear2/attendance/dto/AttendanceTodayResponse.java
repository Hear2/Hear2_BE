package com.hear2.attendance.dto;

import java.time.LocalDate;

public record AttendanceTodayResponse(
        LocalDate attendanceDate,
        boolean myChecked,
        boolean partnerChecked,
        boolean coupleBothChecked
) {
}
