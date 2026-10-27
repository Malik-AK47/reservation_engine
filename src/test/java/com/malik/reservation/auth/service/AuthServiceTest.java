
package com.malik.reservation.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.malik.reservation.auth.dto.LoginRequest;
import com.malik.reservation.auth.dto.RegisterRequest;
import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.InvalidCredentialsException;
import com.malik.reservation.user.entity.AppUser;
import com.malik.reservation.user.entity.UserRole;
import com.malik.reservation.user.repo.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.create("Malik", "malik@example.com", "hashed-password", UserRole.USER);
    }

    @Test
    void shouldRegisterUserAndIssueToken() {
        RegisterRequest request = new RegisterRequest("Malik", " MALIK@Example.com ", "strong-password");

        when(appUserRepository.existsByNormalizedEmail("malik@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("strong-password"))
                .thenReturn("hashed-password");
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("signed-token");
        when(jwtService.getAccessTokenLifetimeSeconds()).thenReturn(900L);

        var response = authService.register(request);

        assertEquals("malik@example.com", response.email());
        assertEquals("USER", response.role());
        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(appUserRepository).saveAndFlush(any(AppUser.class));
    }

    @Test
    void shouldRejectAlreadyRegisteredEmail() {
        when(appUserRepository.existsByNormalizedEmail("malik@example.com"))
                .thenReturn(true);

        RegisterRequest request = new RegisterRequest("Malik", "Malik@example.com", "strong-password");

        assertThrows(BusinessException.class, () -> authService.register(request));

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        when(appUserRepository.findByNormalizedEmail("malik@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("signed-token");
        when(jwtService.getAccessTokenLifetimeSeconds()).thenReturn(900L);

        var response = authService.login(new LoginRequest("MALIK@example.com", "strong-password"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("USER", response.role());
    }

    @Test
    void shouldRejectWrongPassword() {
        when(appUserRepository.findByNormalizedEmail("malik@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password"))
                .thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("malik@example.com", "wrong-password"))
        );

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void shouldRejectUnknownEmailWithGenericError() {
        when(appUserRepository.findByNormalizedEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("unknown@example.com", "password"))
        );
    }
}
