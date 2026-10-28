
package com.malik.reservation.event.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTest {

    private static final Instant START_TIME = Instant.parse("2027-06-15T10:00:00Z");

    private static final Instant END_TIME = Instant.parse("2027-06-15T12:00:00Z");

    private Event createDraft() {
        return Event.createDraft(
                "Tech Conference",
                "A technology conference",
                "Pune",
                START_TIME,
                END_TIME,
                60
        );
    }

    @Test
    void createDraft_shouldInitializeEventWithDraftStatus() {
        Event event = createDraft();

        assertEquals(EventStatus.DRAFT, event.getStatus());
        assertEquals("Tech Conference", event.getName());
        assertEquals("Pune", event.getLocation());
    }

    @Test
    void createDraft_shouldRejectEndTimeBeforeStartTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Event.createDraft(
                        "Tech Conference",
                        null,
                        "Pune",
                        END_TIME,
                        START_TIME,
                        60
                )
        );
    }

    @Test
    void createDraft_shouldRejectEqualStartAndEndTimes() {
        assertThrows(IllegalArgumentException.class, () ->
                Event.createDraft(
                        "Tech Conference",
                        null,
                        "Pune",
                        START_TIME,
                        START_TIME,
                        60
                )
        );
    }

    @Test
    void createDraft_shouldRejectNegativeCancellationCutoff() {
        assertThrows(IllegalArgumentException.class, () ->
                Event.createDraft(
                        "Tech Conference",
                        null,
                        "Pune",
                        START_TIME,
                        END_TIME,
                        -1
                )
        );
    }

    @Test
    void updateDraftDetails_shouldAllowUpdatingDraft() {
        Event event = createDraft();

        event.updateDraftDetails(
                "Updated Conference",
                "Updated description",
                "Mumbai",
                START_TIME,
                END_TIME,
                30
        );

        assertEquals("Updated Conference", event.getName());
        assertEquals("Mumbai", event.getLocation());
        assertEquals(30, event.getCancellationCutoffMinutes());
        assertEquals(EventStatus.DRAFT, event.getStatus());
    }

    @Test
    void publish_shouldTransitionDraftToPublished() {
        Event event = createDraft();

        event.publish();

        assertEquals(EventStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void publish_shouldRejectAlreadyPublishedEvent() {
        Event event = createDraft();
        event.publish();

        assertThrows(IllegalStateException.class, event::publish);
    }

    @Test
    void updatePublishedDetails_shouldAllowPermittedChanges() {
        Event event = createDraft();
        event.publish();

        event.updatePublishedDetails("Updated Conference", "Updated description", "Mumbai");

        assertEquals("Updated Conference", event.getName());
        assertEquals("Updated description", event.getDescription());
        assertEquals("Mumbai", event.getLocation());
        assertEquals(START_TIME, event.getStartTime());
        assertEquals(END_TIME, event.getEndTime());
        assertEquals(60, event.getCancellationCutoffMinutes());
    }

    @Test
    void updateDraftDetails_shouldRejectPublishedEvent() {
        Event event = createDraft();
        event.publish();

        assertThrows(IllegalStateException.class, () ->
                event.updateDraftDetails(
                        "Updated Conference",
                        null,
                        "Mumbai",
                        START_TIME,
                        END_TIME,
                        30
                )
        );
    }

    @Test
    void cancel_shouldTransitionPublishedEventToCancelled() {
        Event event = createDraft();
        event.publish();

        event.cancel();

        assertEquals(EventStatus.CANCELLED, event.getStatus());
    }

    @Test
    void cancel_shouldRejectDraftEvent() {
        Event event = createDraft();

        assertThrows(IllegalStateException.class, event::cancel);
    }

    @Test
    void cancelledEvent_shouldRejectFurtherLifecycleChanges() {
        Event event = createDraft();
        event.publish();
        event.cancel();

        assertThrows(IllegalStateException.class, event::publish);
        assertThrows(IllegalStateException.class, event::cancel);

        assertThrows(IllegalStateException.class, () ->
                event.updatePublishedDetails("Another Name", null, "Mumbai")
        );

        assertThrows(IllegalStateException.class, () ->
                event.updateDraftDetails(
                        "Another Name",
                        null,
                        "Mumbai",
                        START_TIME,
                        END_TIME,
                        30
                )
        );
    }
}
