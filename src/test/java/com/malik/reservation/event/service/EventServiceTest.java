package com.malik.reservation.event.service;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.InvalidRequestException;
import com.malik.reservation.common.exception.ResourceNotFoundException;
import com.malik.reservation.event.dto.CreateEventRequest;
import com.malik.reservation.event.dto.EventResponse;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.EventStatus;
import com.malik.reservation.event.repo.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.malik.reservation.event.dto.UpdateEventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEvent_shouldSaveDraftAndReturnResponse() {
        CreateEventRequest request = new CreateEventRequest(
                "Tech Conference",
                "A technology conference",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );

        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.createEvent(request);

        assertEquals("Tech Conference", response.name());
        assertEquals("A technology conference", response.description());
        assertEquals("Pune", response.location());
        assertEquals(EventStatus.DRAFT, response.status());
        assertEquals(60, response.cancellationCutoffMinutes());

        verify(eventRepository).save(any(Event.class));
    }


    @Test
    void updateDraftEvent_shouldUpdateAllEditableDetails() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Old Event",
                "Old description",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );

        when(eventRepository.findById(eventId))
                .thenReturn(java.util.Optional.of(event));

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Event",
                "Updated description",
                "Mumbai",
                Instant.parse("2027-06-16T10:00:00Z"),
                Instant.parse("2027-06-16T12:00:00Z"),
                90
        );

        EventResponse response = eventService.updateEvent(eventId, request);

        assertEquals("Updated Event", response.name());
        assertEquals("Updated description", response.description());
        assertEquals("Mumbai", response.location());
        assertEquals(Instant.parse("2027-06-16T10:00:00Z"), response.startTime());
        assertEquals(90, response.cancellationCutoffMinutes());
        assertEquals(EventStatus.DRAFT, response.status());
    }

    @Test
    void updatePublishedEvent_shouldPreserveLockedFields() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Old Event",
                "Old description",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );
        event.publish();

        when(eventRepository.findById(eventId))
                .thenReturn(java.util.Optional.of(event));

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Event",
                "Updated description",
                "Mumbai",
                null,
                null,
                null
        );

        EventResponse response = eventService.updateEvent(eventId, request);

        assertEquals("Updated Event", response.name());
        assertEquals("Mumbai", response.location());
        assertEquals(Instant.parse("2027-06-15T10:00:00Z"), response.startTime());
        assertEquals(Instant.parse("2027-06-15T12:00:00Z"), response.endTime());
        assertEquals(60, response.cancellationCutoffMinutes());
        assertEquals(EventStatus.PUBLISHED, response.status());
    }

    @Test
    void updateDraftEvent_withoutRequiredSchedulingFields_shouldRejectRequest() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Event",
                null,
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );

        when(eventRepository.findById(eventId))
                .thenReturn(java.util.Optional.of(event));

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Event",
                null,
                "Mumbai",
                null,
                null,
                null
        );

        assertThrows(InvalidRequestException.class, () -> eventService.updateEvent(eventId, request));
    }


    @Test
    void publishDraftEvent_shouldChangeStatusToPublished() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Tech Conference",
                "Technology event",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );

        when(eventRepository.findById(eventId))
                .thenReturn(java.util.Optional.of(event));

        EventResponse response = eventService.publishEvent(eventId);

        assertEquals(EventStatus.PUBLISHED, response.status());
    }

    @Test
    void publishAlreadyPublishedEvent_shouldRejectTransition() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Tech Conference",
                null,
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );
        event.publish();

        when(eventRepository.findById(eventId))
                .thenReturn(java.util.Optional.of(event));

        assertThrows(BusinessException.class, () -> eventService.publishEvent(eventId));
    }


    @Test
    void getPublishedEvent_shouldReturnPublishedEvent() {
        UUID eventId = UUID.randomUUID();

        Event event = Event.createDraft(
                "Tech Conference",
                "Technology event",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                60
        );
        event.publish();

        when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
                .thenReturn(java.util.Optional.of(event));

        EventResponse response = eventService.getPublishedEvent(eventId);

        assertEquals("Tech Conference", response.name());
        assertEquals(EventStatus.PUBLISHED, response.status());
    }


    @Test
    void getPublishedEvent_whenEventIsNotPublished_shouldReturnNotFound() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
                .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.getPublishedEvent(eventId));
    }



    @Test
    void getPublishedEvents_shouldQueryOnlyPublishedEvents() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Event> emptyPage = Page.empty(pageable);

        when(eventRepository.findAllByStatus(EventStatus.PUBLISHED, pageable))
                .thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getPublishedEvents(pageable);

        assertEquals(0, result.getTotalElements());

        verify(eventRepository).findAllByStatus(EventStatus.PUBLISHED, pageable);
    }

}
