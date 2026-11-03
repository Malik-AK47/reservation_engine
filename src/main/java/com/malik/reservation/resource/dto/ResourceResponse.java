package com.malik.reservation.resource.dto;

import com.malik.reservation.resource.entity.Resource;
import com.malik.reservation.resource.entity.ResourceStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        UUID eventId,
        String resourceCode,
        ResourceStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getEvent().getId(),
                resource.getResourceCode(),
                resource.getStatus(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}