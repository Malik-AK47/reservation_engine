
package com.malik.reservation.event.entity;

import com.malik.reservation.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "cancellation_cutoff_minutes", nullable = false)
    private int cancellationCutoffMinutes;

    public static Event createDraft(String name, String description, String location, Instant startTime, Instant endTime, int cancellationCutoffMinutes) {
        validateDetails(name, location, startTime, endTime, cancellationCutoffMinutes);

        Event event = new Event();
        event.name = name.trim();
        event.description = description;
        event.location = location.trim();
        event.startTime = startTime;
        event.endTime = endTime;
        event.cancellationCutoffMinutes = cancellationCutoffMinutes;
        event.status = EventStatus.DRAFT;

        return event;
    }

    public void updateDraftDetails(String name, String description, String location, Instant startTime, Instant endTime, int cancellationCutoffMinutes) {
        ensureStatus(EventStatus.DRAFT);

        validateDetails(name, location, startTime, endTime, cancellationCutoffMinutes);

        this.name = name.trim();
        this.description = description;
        this.location = location.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.cancellationCutoffMinutes = cancellationCutoffMinutes;
    }

    public void updatePublishedDetails(String name, String description, String location) {
        ensureStatus(EventStatus.PUBLISHED);
        validateText(name, "name", 200);
        validateText(location, "location", 255);

        this.name = name.trim();
        this.description = description;
        this.location = location.trim();
    }

    public void publish() {
        ensureStatus(EventStatus.DRAFT);
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        ensureStatus(EventStatus.PUBLISHED);
        this.status = EventStatus.CANCELLED;
    }

    private void ensureStatus(EventStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("Event must be in " + expectedStatus + " status, but is " + status);
        }
    }

    private static void validateDetails(String name, String location, Instant startTime, Instant endTime, int cancellationCutoffMinutes) {
        validateText(name, "name", 200);
        validateText(location, "location", 255);

        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }

        if (cancellationCutoffMinutes < 0) {
            throw new IllegalArgumentException("Cancellation cutoff must not be negative");
        }
    }

    private static void validateText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
        }
    }
}
