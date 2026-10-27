package com.malik.reservation.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.malik.reservation.auth.dto.AuthResponse;
import com.malik.reservation.auth.dto.LoginRequest;
import com.malik.reservation.auth.dto.RegisterRequest;
import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.InvalidCredentialsException;
import com.malik.reservation.user.entity.AppUser;
import com.malik.reservation.user.entity.UserRole;
import com.malik.reservation.user.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (appUserRepository.existsByNormalizedEmail(normalizedEmail)) {
            throw new BusinessException("Email is already registered");
        }

        validatePasswordLength(request.password());

        AppUser user = AppUser.create(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                UserRole.USER
        );


        AppUser savedUser = appUserRepository.saveAndFlush(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        AppUser user = appUserRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getNormalizedEmail(),
                user.getRole().name(),
                jwtService.generateAccessToken(user),
                "Bearer",
                jwtService.getAccessTokenLifetimeSeconds()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}
