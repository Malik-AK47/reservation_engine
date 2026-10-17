
package com.malik.reservation.reservation.entity;

import com.malik.reservation.event.entity.Resource;
import com.malik.reservation.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
public class Reservation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "resource_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_resource")
    )
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
        // Required by JPA
    }

    public Reservation(User user, Resource resource, Instant expiresAt) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (resource == null) {
            throw new IllegalArgumentException("Resource is required");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiry time is required");
        }

        this.user = user;
        this.resource = resource;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.HELD;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void confirm(Instant decisionTime) {
        if (status != ReservationStatus.HELD) {
            throw new IllegalStateException(
                    "Only a held reservation can be confirmed"
            );
        }

        if (decisionTime == null || !expiresAt.isAfter(decisionTime)) {
            throw new IllegalStateException(
                    "Reservation has expired"
            );
        }

        status = ReservationStatus.CONFIRMED;
        confirmedAt = decisionTime;
    }

    public void cancel(Instant decisionTime) {
        if (status != ReservationStatus.HELD
                && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only an active reservation can be cancelled"
            );
        }

        if (decisionTime == null) {
            throw new IllegalArgumentException(
                    "Decision time is required"
            );
        }

        status = ReservationStatus.CANCELLED;
        cancelledAt = decisionTime;
    }

    public void expire(Instant decisionTime) {
        if (status != ReservationStatus.HELD) {
            throw new IllegalStateException(
                    "Only a held reservation can expire"
            );
        }

        if (decisionTime == null || expiresAt.isAfter(decisionTime)) {
            throw new IllegalStateException(
                    "Reservation has not expired"
            );
        }

        status = ReservationStatus.EXPIRED;
    }
}
