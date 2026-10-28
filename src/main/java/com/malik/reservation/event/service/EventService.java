package com.malik.reservation.event.service;

import com.malik.reservation.common.exception.InvalidRequestException;
import com.malik.reservation.event.dto.CreateEventRequest;
import com.malik.reservation.event.dto.EventResponse;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.repo.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.ResourceNotFoundException;
import com.malik.reservation.event.dto.UpdateEventRequest;
import com.malik.reservation.event.entity.EventStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event;
        try {
            event = Event.createDraft(
                    request.name(),
                    request.description(),
                    request.location(),
                    request.startTime(),
                    request.endTime(),
                    request.cancellationCutoffMinutes()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(exception.getMessage());
        }

        Event savedEvent = eventRepository.save(event);

        return EventResponse.from(savedEvent);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        try {
            if (event.getStatus() == EventStatus.DRAFT) {
                if (request.startTime() == null || request.endTime() == null || request.cancellationCutoffMinutes() == null) {
                    throw new InvalidRequestException("Draft events require start time, end time, " + "and cancellation cutoff");
                }

                try {
                    event.updateDraftDetails(
                            request.name(),
                            request.description(),
                            request.location(),
                            request.startTime(),
                            request.endTime(),
                            request.cancellationCutoffMinutes()
                    );
                } catch (IllegalArgumentException exception) {
                    throw new InvalidRequestException(exception.getMessage());
                }

            } else if (event.getStatus() == EventStatus.PUBLISHED) {
                try {
                    event.updatePublishedDetails(request.name(), request.description(), request.location());
                } catch (IllegalArgumentException exception) {
                    throw new InvalidRequestException(exception.getMessage());
                }

            } else {
                throw new BusinessException("Cancelled events cannot be updated");
            }

        } catch (IllegalStateException exception) {
            throw new BusinessException(exception.getMessage());
        }

        return EventResponse.from(event);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EventResponse publishEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        try {
            event.publish();
        } catch (IllegalStateException exception) {
            throw new BusinessException(exception.getMessage());
        }

        return EventResponse.from(event);
    }


    @Transactional(readOnly = true)
    public EventResponse getPublishedEvent(UUID eventId) {
        Event event = eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getPublishedEvents(Pageable pageable) {
        return eventRepository.findAllByStatus(EventStatus.PUBLISHED, pageable)
                .map(EventResponse::from);
    }

}
