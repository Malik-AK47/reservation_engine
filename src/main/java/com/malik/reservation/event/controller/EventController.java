package com.malik.reservation.event.controller;

import com.malik.reservation.event.dto.EventResponse;
import com.malik.reservation.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public Page<EventResponse> getPublishedEvents(@PageableDefault(size = 10, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return eventService.getPublishedEvents(pageable);
    }

    @GetMapping("/{id}")
    public EventResponse getPublishedEvent(@PathVariable UUID id) {
        return eventService.getPublishedEvent(id);
    }
}
