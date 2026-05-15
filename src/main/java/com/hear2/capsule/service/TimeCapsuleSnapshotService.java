package com.hear2.capsule.service;

import com.hear2.capsule.dto.TimeCapsuleMetricComparisonResponse;
import com.hear2.capsule.dto.TimeCapsuleThenVsNowResponse;
import com.hear2.capsule.entity.TimeCapsuleSnapshot;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeCapsuleSnapshotService {

    private final CoupleRepository coupleRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemoryRepository memoryRepository;

    public TimeCapsuleSnapshot createSnapshot(Long capsuleId, Long coupleId, LocalDateTime snapshotAt) {
        SnapshotMetrics metrics = collectMetrics(coupleId, snapshotAt);

        return TimeCapsuleSnapshot.builder()
                .capsuleId(capsuleId)
                .daysTogether(metrics.daysTogether())
                .messageCount(metrics.messageCount())
                .photoCount(metrics.photoCount())
                .characterLevel(metrics.characterLevel())
                .snapshotAt(snapshotAt)
                .build();
    }

    public TimeCapsuleThenVsNowResponse compare(Long coupleId, TimeCapsuleSnapshot snapshot, LocalDateTime now) {
        if (snapshot == null) {
            return null;
        }

        SnapshotMetrics current = collectMetrics(coupleId, now);
        return TimeCapsuleThenVsNowResponse.builder()
                .daysTogether(compare(snapshot.getDaysTogether(), current.daysTogether(), "일"))
                .messages(compare(snapshot.getMessageCount(), current.messageCount(), "개"))
                .photos(compare(snapshot.getPhotoCount(), current.photoCount(), "장"))
                .characterLevel(compare(snapshot.getCharacterLevel(), current.characterLevel(), "Lv"))
                .build();
    }

    private SnapshotMetrics collectMetrics(Long coupleId, LocalDateTime now) {
        Couple couple = coupleRepository.findById(coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
        long messageCount = chatMessageRepository.countByCoupleId(coupleId);
        long photoCount = memoryRepository.countByCoupleId(coupleId);
        long daysTogether = calculateDaysTogether(couple, now);
        int characterLevel = calculateCharacterLevel(messageCount, photoCount);

        return new SnapshotMetrics(daysTogether, messageCount, photoCount, characterLevel);
    }

    private long calculateDaysTogether(Couple couple, LocalDateTime now) {
        LocalDate startDate = couple.getStartDate();
        if (startDate == null && couple.getCreatedAt() != null) {
            startDate = couple.getCreatedAt().toLocalDate();
        }
        if (startDate == null) {
            return 0;
        }

        return Math.max(0, ChronoUnit.DAYS.between(startDate, now.toLocalDate()) + 1);
    }

    private int calculateCharacterLevel(long messageCount, long photoCount) {
        long activityScore = messageCount + (photoCount * 5);
        return Math.max(1, (int) (activityScore / 50) + 1);
    }

    private TimeCapsuleMetricComparisonResponse compare(long thenValue, long nowValue, String unit) {
        long delta = nowValue - thenValue;
        return TimeCapsuleMetricComparisonResponse.builder()
                .then(format(thenValue, unit))
                .now(format(nowValue, unit))
                .delta(formatSigned(delta, unit))
                .build();
    }

    private String format(long value, String unit) {
        if ("Lv".equals(unit)) {
            return unit + value;
        }
        return value + unit;
    }

    private String formatSigned(long value, String unit) {
        String sign = value > 0 ? "+" : "";
        if ("Lv".equals(unit)) {
            return sign + unit + value;
        }
        return sign + value + unit;
    }

    private record SnapshotMetrics(long daysTogether, long messageCount, long photoCount, int characterLevel) {
    }
}
