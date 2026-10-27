package com.malik.reservation.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String name,
        String email,
        String role,
        String accessToken,
        String tokenType,
        long expiresIn
) {}
