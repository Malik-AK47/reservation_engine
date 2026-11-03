package com.malik.reservation.resource.service;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.InvalidRequestException;
import com.malik.reservation.common.exception.ResourceNotFoundException;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.EventStatus;
import com.malik.reservation.event.repo.EventRepository;
import com.malik.reservation.resource.dto.CreateResourceRequest;
import com.malik.reservation.resource.dto.ResourceResponse;
import com.malik.reservation.resource.entity.Resource;
import com.malik.reservation.resource.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final EventRepository eventRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResourceResponse createResource(UUID eventId, CreateResourceRequest request) {

        String normalizedCode = normalizeResourceCode(request.resourceCode());

        if (normalizedCode.length() > 100) {
            throw new InvalidRequestException("Resource code must not exceed 100 characters after normalization");
        }

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("Resources can only be created for draft events");
        }

        Resource resource = Resource.create(event, normalizedCode);

        Resource savedResource = resourceRepository.saveAndFlush(resource);

        return ResourceResponse.from(savedResource);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResourcesByEvent(UUID eventId) {

        eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return resourceRepository.findByEvent_Id(eventId)
                .stream()
                .map(ResourceResponse::from)
                .toList();
    }

    private String normalizeResourceCode(String resourceCode) {

        if (resourceCode == null || resourceCode.isBlank()) {
            throw new InvalidRequestException("Resource code must not be blank");
        }

        return resourceCode.trim().toUpperCase(Locale.ROOT);
    }
}