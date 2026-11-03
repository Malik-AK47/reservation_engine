package com.malik.reservation.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/v1/test");
    }

    @Test
    void handleDataIntegrityViolation_duplicateEmail_returnsConflict() {

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate email", null, "uq_app_users_normalized_email");

        DataIntegrityViolationException exception = new DataIntegrityViolationException("Database constraint violation", cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMAIL_ALREADY_REGISTERED", response.getBody().code());
        assertEquals("Email is already registered", response.getBody().message());
    }

    @Test
    void handleDataIntegrityViolation_duplicateResourceCode_returnsConflict() {

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate resource code", null, "uq_resources_event_code");

        DataIntegrityViolationException exception = new DataIntegrityViolationException("Database constraint violation", cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_CODE_ALREADY_EXISTS", response.getBody().code());
        assertEquals("Resource code already exists for this event", response.getBody().message());
    }

    @Test
    void handleDataIntegrityViolation_unknownConstraint_returnsInternalServerError() {

        ConstraintViolationException cause = new ConstraintViolationException(
                "Unknown constraint violation", null, "some_unknown_constraint");

        DataIntegrityViolationException exception = new DataIntegrityViolationException("Database constraint violation", cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("An unexpected database error occurred", response.getBody().message());
    }
}