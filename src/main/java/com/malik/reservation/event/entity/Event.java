
package com.malik.reservation.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
public class Event {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // Required by JPA
    }

    public Event(
            String name,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Event name must not be blank"
            );
        }

        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException(
                    "Event start and end times are required"
            );
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(
                    "Event end time must be after start time"
            );
        }

        this.name = name.trim();
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
