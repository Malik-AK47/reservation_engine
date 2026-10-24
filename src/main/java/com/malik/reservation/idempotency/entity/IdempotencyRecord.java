package com.malik.reservation.idempotency.entity;

import com.malik.reservation.common.persistence.BaseEntity;
import com.malik.reservation.reservation.entity.Reservation;
import com.malik.reservation.reservation.entity.ReservationStatus;
import com.malik.reservation.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "operation", nullable = false, length = 50)
    private String operation;

    @Column(name = "key", nullable = false, length = 255)
    private String key;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "result_status", nullable = false)
    private int resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_reservation_status", nullable = false, length = 20)
    private ReservationStatus resultReservationStatus;

    @Column(name = "result_expires_at", nullable = false)
    private Instant resultExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
