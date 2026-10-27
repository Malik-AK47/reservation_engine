
package com.malik.reservation.auth.controller;

import tools.jackson.databind.ObjectMapper;
import com.malik.reservation.auth.dto.AuthResponse;
import com.malik.reservation.auth.dto.LoginRequest;
import com.malik.reservation.auth.dto.RegisterRequest;
import com.malik.reservation.auth.service.AuthService;
import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.GlobalExceptionHandler;
import com.malik.reservation.common.exception.InvalidCredentialsException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Malik", "malik@example.com", "strong-password"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test-token"));
    }

    @Test
    void loginShouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest(
                "malik@example.com", "strong-password"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerShouldRejectInvalidInput() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", "invalid-email", "short"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(authService);
    }

    @Test
    void duplicateEmailShouldReturn409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(
                                        "Malik",
                                        "malik@example.com",
                                        "strong-password"
                                ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void invalidCredentialsShouldReturn401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(
                                        "malik@example.com",
                                        "wrong-password"
                                ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private AuthResponse response() {
        return new AuthResponse(
                UUID.randomUUID(),
                "Malik",
                "malik@example.com",
                "USER",
                "test-token",
                "Bearer",
                900L
        );
    }
}
