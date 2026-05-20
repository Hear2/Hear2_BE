package com.hear2.attendance.service;

import com.hear2.attendance.dto.AttendanceCheckResponse;
import com.hear2.attendance.dto.AttendanceTodayResponse;
import com.hear2.attendance.entity.AttendanceCheck;
import com.hear2.attendance.repository.AttendanceCheckRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceCheckRepository attendanceCheckRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public AttendanceCheckResponse checkToday(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        LocalDate today = LocalDate.now();
        boolean alreadyChecked = attendanceCheckRepository.existsByUserIdAndAttendanceDate(userId, today);

        if (!alreadyChecked) {
            attendanceCheckRepository.save(AttendanceCheck.builder()
                    .userId(userId)
                    .coupleId(coupleId)
                    .attendanceDate(today)
                    .build());
        }

        boolean coupleBothChecked = attendanceCheckRepository.countByCoupleIdAndAttendanceDate(coupleId, today) >= 2;

        return new AttendanceCheckResponse(
                today,
                true,
                alreadyChecked,
                coupleBothChecked
        );
    }

    @Transactional(readOnly = true)
    public AttendanceTodayResponse getToday(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        LocalDate today = LocalDate.now();
        boolean myChecked = attendanceCheckRepository.existsByUserIdAndAttendanceDate(userId, today);
        boolean partnerChecked = attendanceCheckRepository.existsByCoupleIdAndAttendanceDateAndUserIdNot(
                coupleId,
                today,
                userId
        );

        return new AttendanceTodayResponse(
                today,
                myChecked,
                partnerChecked,
                myChecked && partnerChecked
        );
    }

    private Long resolveCoupleId(Long userId) {
        CoupleMember member = coupleMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not connected"));
        return member.getCoupleId();
    }
}
