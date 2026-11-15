
package com.malik.reservation.resource.entity;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.event.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTest {

    private Resource resource;

    @BeforeEach
    void setUp() {
        Event event = Event.createDraft(
                "Test Event",
                "Test Description",
                "Mumbai",
                Instant.parse("2026-12-01T10:00:00Z"),
                Instant.parse("2026-12-01T12:00:00Z"),
                30
        );

        resource = Resource.create(event, "A-101");
    }

    @Test
    void create_shouldSetAvailableStatus() {
        assertEquals(ResourceStatus.AVAILABLE, resource.getStatus());
    }

    @Test
    void hold_whenAvailable_shouldSetHeldStatus() {
        resource.hold();

        assertEquals(ResourceStatus.HELD, resource.getStatus());
    }

    @Test
    void hold_whenAlreadyHeld_shouldRejectTransition() {
        resource.hold();

        assertThrows(BusinessException.class, resource::hold);
    }

    @Test
    void book_whenHeld_shouldSetBookedStatus() {
        resource.hold();
        resource.book();

        assertEquals(ResourceStatus.BOOKED, resource.getStatus());
    }

    @Test
    void book_whenAvailable_shouldRejectTransition() {
        assertThrows(BusinessException.class, resource::book);
    }

    @Test
    void release_whenHeld_shouldSetAvailableStatus() {
        resource.hold();
        resource.release();

        assertEquals(ResourceStatus.AVAILABLE, resource.getStatus());
    }

    @Test
    void release_whenBooked_shouldSetAvailableStatus() {
        resource.hold();
        resource.book();
        resource.release();

        assertEquals(ResourceStatus.AVAILABLE, resource.getStatus());
    }

    @Test
    void release_whenAvailable_shouldRejectTransition() {
        assertThrows(BusinessException.class, resource::release);
    }
}
