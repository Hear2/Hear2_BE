package com.hear2.calendar.repository;

import com.hear2.calendar.entity.CalendarEventMemoryLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarEventMemoryLinkRepository extends JpaRepository<CalendarEventMemoryLink, Long> {

    boolean existsByEventIdAndMemoryId(Long eventId, Long memoryId);

    Optional<CalendarEventMemoryLink> findByCoupleIdAndEventIdAndMemoryId(Long coupleId, Long eventId, Long memoryId);

    List<CalendarEventMemoryLink> findByCoupleIdAndEventIdOrderByCreatedAtAsc(Long coupleId, Long eventId);

    List<CalendarEventMemoryLink> findByCoupleIdAndMemoryIdOrderByCreatedAtAsc(Long coupleId, Long memoryId);

    List<CalendarEventMemoryLink> findByCoupleIdAndEventIdIn(Long coupleId, List<Long> eventIds);

    void deleteByCoupleIdAndEventId(Long coupleId, Long eventId);
}
