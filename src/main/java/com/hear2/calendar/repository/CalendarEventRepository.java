package com.hear2.calendar.repository;

import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarExternalProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    Optional<CalendarEvent> findByIdAndCoupleId(Long id, Long coupleId);

    Optional<CalendarEvent> findByCoupleIdAndExternalProviderAndExternalEventId(
            Long coupleId,
            CalendarExternalProvider externalProvider,
            String externalEventId
    );

    @Query("""
            select event
            from CalendarEvent event
            where event.coupleId = :coupleId
              and event.startsAt <= :rangeEnd
              and event.endsAt >= :rangeStart
            order by event.startsAt asc, event.id asc
            """)
    List<CalendarEvent> findEventsInRange(
            @Param("coupleId") Long coupleId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    @Query("""
            select event
            from CalendarEvent event
            where event.coupleId = :coupleId
              and (
                    (event.startsAt <= :rangeEnd and event.endsAt >= :rangeStart)
                    or (event.recurrenceRule is not null and event.startsAt <= :rangeEnd)
                  )
            order by event.startsAt asc, event.id asc
            """)
    List<CalendarEvent> findEventCandidatesInRange(
            @Param("coupleId") Long coupleId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    List<CalendarEvent> findByCoupleIdAndStartsAtGreaterThanEqualOrderByStartsAtAscIdAsc(
            Long coupleId,
            LocalDateTime startsAt
    );
}
