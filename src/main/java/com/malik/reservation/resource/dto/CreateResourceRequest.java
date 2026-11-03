package com.malik.reservation.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(

        @NotBlank(message = "Resource code must not be blank")
        @Size(max = 100, message = "Resource code must not exceed 100 characters")
        String resourceCode

) {}