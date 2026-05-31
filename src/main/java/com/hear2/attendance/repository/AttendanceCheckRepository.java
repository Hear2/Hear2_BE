package com.hear2.attendance.repository;

import com.hear2.attendance.entity.AttendanceCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AttendanceCheckRepository extends JpaRepository<AttendanceCheck, Long> {

    boolean existsByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    long countByCoupleIdAndAttendanceDate(Long coupleId, LocalDate attendanceDate);

    boolean existsByCoupleIdAndAttendanceDateAndUserIdNot(
            Long coupleId,
            LocalDate attendanceDate,
            Long userId
    );
}
