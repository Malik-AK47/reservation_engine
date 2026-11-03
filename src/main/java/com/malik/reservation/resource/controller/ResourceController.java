package com.malik.reservation.resource.controller;

import com.malik.reservation.resource.dto.ResourceResponse;
import com.malik.reservation.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getResourcesByEvent(@PathVariable UUID eventId) {

        return ResponseEntity.ok(resourceService.getResourcesByEvent(eventId));
    }
}