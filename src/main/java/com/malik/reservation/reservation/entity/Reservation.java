
package com.malik.reservation.reservation.entity;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.persistence.AuditableEntity;
import com.malik.reservation.resource.entity.Resource;
import com.malik.reservation.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "held_at", nullable = false)
    private Instant heldAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public static Reservation createHold(AppUser user, Resource resource, Instant heldAt, Instant expiresAt) {
        if (user == null || resource == null || heldAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Reservation creation arguments must not be null");
        }

        if (!expiresAt.isAfter(heldAt)) {
            throw new IllegalArgumentException("Expiry time must be after held time");
        }

        Reservation reservation = new Reservation();
        reservation.user = user;
        reservation.resource = resource;
        reservation.status = ReservationStatus.HELD;
        reservation.heldAt = heldAt;
        reservation.expiresAt = expiresAt;

        return reservation;
    }

    public void confirm(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time must not be null");
        }

        if (status != ReservationStatus.HELD) {
            throw new BusinessException("Only held reservations can be confirmed");
        }

        if (!now.isBefore(expiresAt)) {
            throw new BusinessException("Reservation hold has expired");
        }

        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void expire(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time must not be null");
        }

        if (status != ReservationStatus.HELD) {
            throw new BusinessException("Only held reservations can expire");
        }

        if (now.isBefore(expiresAt)) {
            throw new BusinessException("Reservation hold has not expired yet");
        }

        this.status = ReservationStatus.EXPIRED;
    }

    public void cancel(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time must not be null");
        }

        if (status != ReservationStatus.HELD && status != ReservationStatus.CONFIRMED) {
            throw new BusinessException("Only held or confirmed reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }
}
