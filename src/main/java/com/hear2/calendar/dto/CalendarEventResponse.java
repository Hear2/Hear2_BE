package com.hear2.calendar.dto;

import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarEventViewType;
import com.hear2.calendar.entity.CalendarEventVisibility;
import com.hear2.calendar.entity.CalendarExternalProvider;
import com.hear2.memory.dto.MemoryResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Builder
public record CalendarEventResponse(
        Long id,
        Long coupleId,
        Long ownerId,
        Long createdBy,
        String title,
        CalendarEventVisibility visibility,
        CalendarEventViewType viewType,
        CalendarEventColor color,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean allDay,
        String locationName,
        String addressName,
        BigDecimal latitude,
        BigDecimal longitude,
        String memo,
        List<String> tags,
        String recurrenceRule,
        Integer remindBeforeMinutes,
        Long linkedChatMessageId,
        List<MemoryResponse> linkedMemories,
        CalendarExternalProvider externalProvider,
        String externalEventId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CalendarEventResponse from(
            CalendarEvent event,
            CalendarEventViewType viewType,
            List<MemoryResponse> linkedMemories
    ) {
        return CalendarEventResponse.builder()
                .id(event.getId())
                .coupleId(event.getCoupleId())
                .ownerId(event.getOwnerId())
                .createdBy(event.getCreatedBy())
                .title(event.getTitle())
                .visibility(event.getVisibility())
                .viewType(viewType)
                .color(CalendarEventColor.from(viewType))
                .startsAt(toUtc(event.getStartsAt()))
                .endsAt(toUtc(event.getEndsAt()))
                .allDay(event.isAllDay())
                .locationName(event.getLocationName())
                .addressName(event.getAddressName())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .memo(event.getMemo())
                .tags(List.copyOf(event.getTags()))
                .recurrenceRule(event.getRecurrenceRule())
                .remindBeforeMinutes(event.getRemindBeforeMinutes())
                .linkedChatMessageId(event.getLinkedChatMessageId())
                .linkedMemories(linkedMemories == null ? List.of() : linkedMemories)
                .externalProvider(event.getExternalProvider())
                .externalEventId(event.getExternalEventId())
                .createdAt(toUtc(event.getCreatedAt()))
                .updatedAt(toUtc(event.getUpdatedAt()))
                .build();
    }

    private static OffsetDateTime toUtc(LocalDateTime dateTime) {
        return dateTime == null ? null : OffsetDateTime.of(dateTime, ZoneOffset.UTC);
    }
}
