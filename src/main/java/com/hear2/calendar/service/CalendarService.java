package com.hear2.calendar.service;

import com.hear2.calendar.dto.CalendarDateDetailResponse;
import com.hear2.calendar.dto.CalendarEventCreateRequest;
import com.hear2.calendar.dto.CalendarEventResponse;
import com.hear2.calendar.dto.CalendarEventSummaryResponse;
import com.hear2.calendar.dto.CalendarEventTarget;
import com.hear2.calendar.dto.CalendarEventUpdateRequest;
import com.hear2.calendar.dto.CalendarLegendResponse;
import com.hear2.calendar.dto.CalendarMemoryMarkerResponse;
import com.hear2.calendar.dto.CalendarMonthDayResponse;
import com.hear2.calendar.dto.CalendarMonthResponse;
import com.hear2.calendar.dto.CalendarUpcomingResponse;
import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarEventMemoryLink;
import com.hear2.calendar.entity.CalendarEventViewType;
import com.hear2.calendar.entity.CalendarEventVisibility;
import com.hear2.calendar.repository.CalendarEventMemoryLinkRepository;
import com.hear2.calendar.repository.CalendarEventRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.memory.dto.MemoryCalendarDayResponse;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final int DEFAULT_UPCOMING_LIMIT = 10;
    private static final int MAX_UPCOMING_LIMIT = 50;

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventMemoryLinkRepository calendarEventMemoryLinkRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final MemoryRepository memoryRepository;

    @Transactional
    public CalendarEventResponse createEvent(CalendarEventCreateRequest request, Long currentUserId) {
        validateCreateRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        LocalDateTime startsAt = toUtcDateTime(request.getStartsAt(), "startsAt is required");
        LocalDateTime endsAt = toUtcDateTimeOrDefault(request.getEndsAt(), startsAt);
        validateDateRange(startsAt, endsAt);

        EventOwnership ownership = resolveOwnership(request.getTarget(), context);
        CalendarEvent event = CalendarEvent.builder()
                .coupleId(context.coupleId())
                .ownerId(ownership.ownerId())
                .createdBy(context.userId())
                .title(normalizeRequired(request.getTitle(), "title is required"))
                .visibility(ownership.visibility())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .allDay(request.isAllDay())
                .locationName(normalize(request.getLocationName()))
                .addressName(normalize(request.getAddressName()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .memo(normalize(request.getMemo()))
                .recurrenceRule(normalize(request.getRecurrenceRule()))
                .remindBeforeMinutes(request.getRemindBeforeMinutes())
                .linkedChatMessageId(request.getLinkedChatMessageId())
                .build();
        event.replaceTags(normalizeTags(request.getTags()));

        CalendarEvent savedEvent = calendarEventRepository.save(event);
        linkMemories(context.coupleId(), savedEvent.getId(), request.getMemoryIds());

        return toEventResponse(savedEvent, context.userId());
    }

    @Transactional(readOnly = true)
    public CalendarMonthResponse getMonth(int year, int month, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        YearMonth yearMonth = validateYearMonth(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDateTime rangeStart = monthStart.atStartOfDay();
        LocalDateTime rangeEnd = monthEnd.atTime(23, 59, 59);

        List<CalendarEvent> events = calendarEventRepository.findEventsInRange(
                context.coupleId(),
                rangeStart,
                rangeEnd
        );
        Map<LocalDate, List<CalendarEventSummaryResponse>> eventsByDate = expandEventsByDate(
                events,
                monthStart,
                monthEnd,
                context.userId()
        );
        Map<LocalDate, MemoryCalendarDayResponse> memoriesByDate = getMemoryCalendarDays(
                context.coupleId(),
                monthStart,
                monthEnd
        );

        List<CalendarMonthDayResponse> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            days.add(CalendarMonthDayResponse.builder()
                    .date(date)
                    .events(eventsByDate.getOrDefault(date, List.of()))
                    .memoryMarker(CalendarMemoryMarkerResponse.from(memoriesByDate.get(date)))
                    .build());
        }

        return CalendarMonthResponse.builder()
                .year(year)
                .month(month)
                .days(days)
                .legend(CalendarLegendResponse.defaults())
                .build();
    }

    @Transactional(readOnly = true)
    public CalendarDateDetailResponse getDateDetail(LocalDate date, Long currentUserId) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }

        CoupleContext context = resolveCoupleContext(currentUserId);
        List<CalendarEventResponse> events = calendarEventRepository.findEventsInRange(
                        context.coupleId(),
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59)
                )
                .stream()
                .map(event -> toEventResponse(event, context.userId()))
                .toList();
        List<MemoryResponse> memories = memoryRepository
                .findByCoupleIdAndMemoryDateOrderByCreatedAtDesc(context.coupleId(), date)
                .stream()
                .map(MemoryResponse::from)
                .toList();

        return CalendarDateDetailResponse.builder()
                .date(date)
                .events(events)
                .memories(memories)
                .build();
    }

    @Transactional(readOnly = true)
    public CalendarUpcomingResponse getUpcoming(Integer limit, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        int normalizedLimit = normalizeUpcomingLimit(limit);

        List<CalendarEventSummaryResponse> events = calendarEventRepository
                .findByCoupleIdAndStartsAtGreaterThanEqualOrderByStartsAtAscIdAsc(
                        context.coupleId(),
                        LocalDateTime.now(ZoneOffset.UTC)
                )
                .stream()
                .limit(normalizedLimit)
                .map(event -> CalendarEventSummaryResponse.from(event, resolveViewType(event, context.userId())))
                .toList();

        return CalendarUpcomingResponse.builder()
                .events(events)
                .build();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse getEvent(Long eventId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        return toEventResponse(findEvent(context.coupleId(), eventId), context.userId());
    }

    @Transactional
    public CalendarEventResponse updateEvent(Long eventId, CalendarEventUpdateRequest request, Long currentUserId) {
        validateUpdateRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        CalendarEvent event = findEvent(context.coupleId(), eventId);
        LocalDateTime startsAt = toUtcDateTime(request.getStartsAt(), "startsAt is required");
        LocalDateTime endsAt = toUtcDateTimeOrDefault(request.getEndsAt(), startsAt);
        validateDateRange(startsAt, endsAt);
        EventOwnership ownership = resolveOwnership(request.getTarget(), context);

        event.update(
                normalizeRequired(request.getTitle(), "title is required"),
                ownership.ownerId(),
                ownership.visibility(),
                startsAt,
                endsAt,
                request.isAllDay(),
                normalize(request.getLocationName()),
                normalize(request.getAddressName()),
                request.getLatitude(),
                request.getLongitude(),
                normalize(request.getMemo()),
                normalizeTags(request.getTags()),
                normalize(request.getRecurrenceRule()),
                request.getRemindBeforeMinutes(),
                request.getLinkedChatMessageId()
        );

        return toEventResponse(event, context.userId());
    }

    @Transactional
    public void deleteEvent(Long eventId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        CalendarEvent event = findEvent(context.coupleId(), eventId);

        calendarEventMemoryLinkRepository.deleteByCoupleIdAndEventId(context.coupleId(), event.getId());
        calendarEventRepository.delete(event);
    }

    @Transactional
    public CalendarEventResponse linkMemory(Long eventId, Long memoryId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        CalendarEvent event = findEvent(context.coupleId(), eventId);
        validateMemory(context.coupleId(), memoryId);
        createMemoryLink(context.coupleId(), event.getId(), memoryId);

        return toEventResponse(event, context.userId());
    }

    @Transactional
    public CalendarEventResponse unlinkMemory(Long eventId, Long memoryId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        CalendarEvent event = findEvent(context.coupleId(), eventId);
        CalendarEventMemoryLink link = calendarEventMemoryLinkRepository
                .findByCoupleIdAndEventIdAndMemoryId(context.coupleId(), event.getId(), memoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "memory link not found"));

        calendarEventMemoryLinkRepository.delete(link);
        return toEventResponse(event, context.userId());
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEventsByMemory(Long memoryId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        validateMemory(context.coupleId(), memoryId);

        List<Long> eventIds = calendarEventMemoryLinkRepository
                .findByCoupleIdAndMemoryIdOrderByCreatedAtAsc(context.coupleId(), memoryId)
                .stream()
                .map(CalendarEventMemoryLink::getEventId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, CalendarEvent> eventsById = calendarEventRepository.findAllById(eventIds)
                .stream()
                .filter(event -> Objects.equals(event.getCoupleId(), context.coupleId()))
                .collect(Collectors.toMap(CalendarEvent::getId, Function.identity()));

        return eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .map(event -> toEventResponse(event, context.userId()))
                .toList();
    }

    private CalendarEventResponse toEventResponse(CalendarEvent event, Long currentUserId) {
        List<MemoryResponse> linkedMemories = findLinkedMemories(event.getCoupleId(), event.getId());
        return CalendarEventResponse.from(event, resolveViewType(event, currentUserId), linkedMemories);
    }

    private List<MemoryResponse> findLinkedMemories(Long coupleId, Long eventId) {
        List<Long> memoryIds = calendarEventMemoryLinkRepository
                .findByCoupleIdAndEventIdOrderByCreatedAtAsc(coupleId, eventId)
                .stream()
                .map(CalendarEventMemoryLink::getMemoryId)
                .toList();
        if (memoryIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Memory> memoriesById = memoryRepository.findAllById(memoryIds)
                .stream()
                .filter(memory -> Objects.equals(memory.getCoupleId(), coupleId))
                .collect(Collectors.toMap(Memory::getId, Function.identity()));

        return memoryIds.stream()
                .map(memoriesById::get)
                .filter(Objects::nonNull)
                .map(MemoryResponse::from)
                .toList();
    }

    private void linkMemories(Long coupleId, Long eventId, List<Long> memoryIds) {
        normalizeMemoryIds(memoryIds).forEach(memoryId -> {
            validateMemory(coupleId, memoryId);
            createMemoryLink(coupleId, eventId, memoryId);
        });
    }

    private void createMemoryLink(Long coupleId, Long eventId, Long memoryId) {
        if (calendarEventMemoryLinkRepository.existsByEventIdAndMemoryId(eventId, memoryId)) {
            return;
        }

        calendarEventMemoryLinkRepository.save(CalendarEventMemoryLink.builder()
                .coupleId(coupleId)
                .eventId(eventId)
                .memoryId(memoryId)
                .build());
    }

    private void validateMemory(Long coupleId, Long memoryId) {
        if (memoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memoryId is required");
        }
        memoryRepository.findByIdAndCoupleId(memoryId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "memory not found"));
    }

    private CalendarEvent findEvent(Long coupleId, Long eventId) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }

        return calendarEventRepository.findByIdAndCoupleId(eventId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "calendar event not found"));
    }

    private Map<LocalDate, List<CalendarEventSummaryResponse>> expandEventsByDate(
            List<CalendarEvent> events,
            LocalDate monthStart,
            LocalDate monthEnd,
            Long currentUserId
    ) {
        Map<LocalDate, List<CalendarEventSummaryResponse>> eventsByDate = new LinkedHashMap<>();

        for (CalendarEvent event : events) {
            LocalDate startDate = event.getStartsAt().toLocalDate().isBefore(monthStart)
                    ? monthStart
                    : event.getStartsAt().toLocalDate();
            LocalDate endDate = event.getEndsAt().toLocalDate().isAfter(monthEnd)
                    ? monthEnd
                    : event.getEndsAt().toLocalDate();
            CalendarEventSummaryResponse summary = CalendarEventSummaryResponse.from(
                    event,
                    resolveViewType(event, currentUserId)
            );

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                eventsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(summary);
            }
        }

        eventsByDate.values().forEach(dayEvents -> dayEvents.sort(Comparator.comparing(CalendarEventSummaryResponse::startsAt)));
        return eventsByDate;
    }

    private Map<LocalDate, MemoryCalendarDayResponse> getMemoryCalendarDays(
            Long coupleId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return memoryRepository
                .findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
                        coupleId,
                        startDate,
                        endDate
                )
                .stream()
                .collect(Collectors.groupingBy(
                        Memory::getMemoryDate,
                        Collectors.collectingAndThen(Collectors.toList(), memories -> MemoryCalendarDayResponse.builder()
                                .date(memories.get(0).getMemoryDate())
                                .thumbnails(memories.stream()
                                        .limit(3)
                                        .map(MemoryResponse::resolvePhotoUrl)
                                        .toList())
                                .memoryCount(memories.size())
                                .dominantEmoji("HEART")
                                .build())
                ));
    }

    private CalendarEventViewType resolveViewType(CalendarEvent event, Long currentUserId) {
        if (event.getVisibility() == CalendarEventVisibility.SHARED) {
            return CalendarEventViewType.SHARED;
        }
        if (Objects.equals(event.getOwnerId(), currentUserId)) {
            return CalendarEventViewType.OWNER;
        }
        return CalendarEventViewType.PARTNER;
    }

    private EventOwnership resolveOwnership(CalendarEventTarget target, CoupleContext context) {
        CalendarEventTarget normalizedTarget = target == null ? CalendarEventTarget.SHARED : target;
        return switch (normalizedTarget) {
            case MINE -> new EventOwnership(context.userId(), CalendarEventVisibility.PERSONAL);
            case PARTNER -> new EventOwnership(resolvePartnerId(context), CalendarEventVisibility.PERSONAL);
            case SHARED -> new EventOwnership(context.userId(), CalendarEventVisibility.SHARED);
        };
    }

    private Long resolvePartnerId(CoupleContext context) {
        return coupleMemberRepository.findFirstByCoupleIdAndUserIdNot(context.coupleId(), context.userId())
                .map(CoupleMember::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "partner is required"));
    }

    private CoupleContext resolveCoupleContext(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));
        return new CoupleContext(member.getCoupleId(), currentUserId);
    }

    private void validateCreateRequest(CalendarEventCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "calendar event request is required");
        }
    }

    private void validateUpdateRequest(CalendarEventUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "calendar event request is required");
        }
    }

    private YearMonth validateYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year and month must be valid");
        }
    }

    private LocalDateTime toUtcDateTime(OffsetDateTime dateTime, String message) {
        if (dateTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC);
    }

    private LocalDateTime toUtcDateTimeOrDefault(OffsetDateTime dateTime, LocalDateTime defaultValue) {
        if (dateTime == null) {
            return defaultValue;
        }

        return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC);
    }

    private void validateDateRange(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (endsAt.isBefore(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
    }

    private int normalizeUpcomingLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_UPCOMING_LIMIT;
        }
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be positive");
        }

        return Math.min(limit, MAX_UPCOMING_LIMIT);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
                .filter(StringUtils::hasText)
                .map(tag -> tag.replace("#", "").trim())
                .filter(StringUtils::hasText)
                .distinct()
                .limit(20)
                .toList();
    }

    private List<Long> normalizeMemoryIds(List<Long> memoryIds) {
        if (memoryIds == null) {
            return List.of();
        }

        return memoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(20)
                .toList();
    }

    private record CoupleContext(Long coupleId, Long userId) {
    }

    private record EventOwnership(Long ownerId, CalendarEventVisibility visibility) {
    }
}
