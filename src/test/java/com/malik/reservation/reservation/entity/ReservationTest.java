
package com.malik.reservation.reservation.entity;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.resource.entity.Resource;
import com.malik.reservation.user.entity.AppUser;
import com.malik.reservation.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private AppUser user;
    private Resource resource;
    private Reservation reservation;

    private final Instant heldAt = Instant.parse("2026-11-15T10:00:00Z");

    private final Instant expiresAt = Instant.parse("2026-11-15T10:05:00Z");

    @BeforeEach
    void setUp() {
        user = AppUser.create("Test User", "test@example.com", "test-password-hash", UserRole.USER);

        Event event = Event.createDraft(
                "Test Event",
                "Test Description",
                "Test Venue, Mumbai",
                Instant.parse("2026-12-01T10:00:00Z"),
                Instant.parse("2026-12-01T12:00:00Z"),
                30
        );

        resource = Resource.create(event, "A-101");

        reservation = Reservation.createHold(user, resource, heldAt, expiresAt);
    }

    @Test
    void createHold_shouldCreateHeldReservation() {
        assertEquals(ReservationStatus.HELD, reservation.getStatus());
        assertEquals(heldAt, reservation.getHeldAt());
        assertEquals(expiresAt, reservation.getExpiresAt());
        assertNull(reservation.getConfirmedAt());
        assertNull(reservation.getCancelledAt());
    }

    @Test
    void confirm_beforeExpiry_shouldConfirmReservation() {
        Instant now = Instant.parse("2026-11-15T10:03:00Z");

        reservation.confirm(now);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(now, reservation.getConfirmedAt());
    }

    @Test
    void confirm_atExpiry_shouldRejectConfirmation() {
        assertThrows(BusinessException.class,
                () -> reservation.confirm(expiresAt));
    }

    @Test
    void expire_atExpiry_shouldExpireReservation() {
        reservation.expire(expiresAt);

        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
    }

    @Test
    void expire_beforeExpiry_shouldRejectExpiration() {
        Instant now = Instant.parse("2026-11-15T10:03:00Z");

        assertThrows(BusinessException.class,
                () -> reservation.expire(now));
    }

    @Test
    void cancelHeldReservation_shouldCancelReservation() {
        Instant now = Instant.parse("2026-11-15T10:02:00Z");

        reservation.cancel(now);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(now, reservation.getCancelledAt());
    }

    @Test
    void cancelConfirmedReservation_shouldCancelReservation() {
        Instant confirmedAt = Instant.parse("2026-11-15T10:02:00Z");
        Instant cancelledAt = Instant.parse("2026-11-15T10:03:00Z");

        reservation.confirm(confirmedAt);
        reservation.cancel(cancelledAt);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(cancelledAt, reservation.getCancelledAt());
    }

    @Test
    void confirmExpiredReservation_shouldRejectTransition() {
        reservation.expire(expiresAt);

        assertThrows(BusinessException.class,
                () -> reservation.confirm(Instant.parse("2026-11-15T10:06:00Z")));
    }

    @Test
    void cancelExpiredReservation_shouldRejectTransition() {
        reservation.expire(expiresAt);

        assertThrows(BusinessException.class,
                () -> reservation.cancel(Instant.parse("2026-11-15T10:06:00Z")));
    }
}
