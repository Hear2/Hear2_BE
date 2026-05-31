package com.hear2.attendance.service;

import com.hear2.attendance.dto.AttendanceCheckResponse;
import com.hear2.attendance.dto.AttendanceTodayResponse;
import com.hear2.attendance.entity.AttendanceCheck;
import com.hear2.attendance.repository.AttendanceCheckRepository;
import com.hear2.character.service.CharacterService;
import com.hear2.character.support.CharacterExpSourceType;
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

    private static final long ATTENDANCE_EXP = 10L;
    private static final long ATTENDANCE_BONUS_EXP = 10L;

    private final AttendanceCheckRepository attendanceCheckRepository;
    private final CharacterService characterService;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public AttendanceCheckResponse checkToday(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        LocalDate today = LocalDate.now();
        boolean alreadyChecked = attendanceCheckRepository.existsByUserIdAndAttendanceDate(userId, today);

        if (!alreadyChecked) {
            AttendanceCheck attendanceCheck = attendanceCheckRepository.save(AttendanceCheck.builder()
                    .userId(userId)
                    .coupleId(coupleId)
                    .attendanceDate(today)
                    .build());
            characterService.grantExp(
                    coupleId,
                    CharacterExpSourceType.ATTENDANCE,
                    "ATTENDANCE:" + attendanceCheck.getAttendanceId(),
                    ATTENDANCE_EXP
            );
        }

        boolean coupleBothChecked = attendanceCheckRepository.countByCoupleIdAndAttendanceDate(coupleId, today) >= 2;
        if (!alreadyChecked && coupleBothChecked) {
            characterService.grantExp(
                    coupleId,
                    CharacterExpSourceType.ATTENDANCE,
                    "ATTENDANCE_BONUS:" + coupleId + ":" + today,
                    ATTENDANCE_BONUS_EXP
            );
        }

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
