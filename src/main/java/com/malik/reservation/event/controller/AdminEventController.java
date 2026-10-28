package com.malik.reservation.event.controller;

import com.malik.reservation.event.dto.CreateEventRequest;
import com.malik.reservation.event.dto.EventResponse;
import com.malik.reservation.event.dto.UpdateEventRequest;
import com.malik.reservation.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        EventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable UUID id) {
        EventResponse response = eventService.publishEvent(id);
        return ResponseEntity.ok(response);
    }
}
