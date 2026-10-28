
package com.malik.reservation.event.dto;

import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String description,
        String location,
        Instant startTime,
        Instant endTime,
        EventStatus status,
        int cancellationCutoffMinutes,
        Instant createdAt,
        Instant updatedAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getStatus(),
                event.getCancellationCutoffMinutes(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
