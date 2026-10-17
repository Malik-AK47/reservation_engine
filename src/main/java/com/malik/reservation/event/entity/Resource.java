
package com.malik.reservation.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "resources",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_resources_event_code",
                columnNames = {"event_id", "code"}
        )
)
@Getter
public class Resource {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Resource() {
        // Required by JPA
    }

    public Resource(Event event, String code) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Resource must belong to an event"
            );
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Resource code must not be blank"
            );
        }

        this.event = event;
        this.code = code.trim();
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


    public void markHeld() {
        if (status != ResourceStatus.AVAILABLE) {
            throw new IllegalStateException("Only an available resource can be held");
        }

        status = ResourceStatus.HELD;
    }

    public void markBooked() {
        if (status != ResourceStatus.HELD) {
            throw new IllegalStateException("Only a held resource can be booked");
        }

        status = ResourceStatus.BOOKED;
    }

    public void markAvailable() {
        if (status != ResourceStatus.HELD && status != ResourceStatus.BOOKED) {
            throw new IllegalStateException("Only an occupied resource can become available");
        }

        status = ResourceStatus.AVAILABLE;
    }

}
