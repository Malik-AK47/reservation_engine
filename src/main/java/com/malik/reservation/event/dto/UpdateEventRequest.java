
package com.malik.reservation.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateEventRequest(

        @NotBlank(message = "Event name is required")
        @Size(max = 200, message = "Event name must not exceed 200 characters")
        String name,

        String description,

        @NotBlank(message = "Event location is required")
        @Size(max = 255, message = "Event location must not exceed 255 characters")
        String location,

        Instant startTime,

        Instant endTime,

        @PositiveOrZero(message = "Cancellation cutoff must not be negative")
        Integer cancellationCutoffMinutes

) {
}
