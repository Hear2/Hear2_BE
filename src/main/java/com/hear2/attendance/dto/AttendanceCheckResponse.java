package com.hear2.attendance.dto;

import java.time.LocalDate;

public record AttendanceCheckResponse(
        LocalDate attendanceDate,
        boolean checked,
        boolean alreadyChecked,
        boolean coupleBothChecked
) {
}
