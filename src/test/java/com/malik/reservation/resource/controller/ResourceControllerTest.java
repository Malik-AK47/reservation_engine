package com.malik.reservation.resource.controller;

import com.malik.reservation.common.config.SecurityConfig;
import com.malik.reservation.resource.dto.ResourceResponse;
import com.malik.reservation.resource.entity.ResourceStatus;
import com.malik.reservation.resource.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourceController.class)
@Import(SecurityConfig.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID eventId =
            UUID.fromString("1a4307b1-c707-4dfd-a753-15be1f822f0d");

    @Test
    void anonymousUser_canGetResourcesForPublishedEvent() throws Exception {

        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T10:00:00Z");

        ResourceResponse response = new ResourceResponse(resourceId, eventId, "A-101", ResourceStatus.AVAILABLE, now, now);

        when(resourceService.getResourcesByEvent(eventId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/events/{eventId}/resources", eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(resourceId.toString()))
                .andExpect(jsonPath("$[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$[0].resourceCode").value("A-101"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(resourceService).getResourcesByEvent(eventId);
    }

    @Test
    void anonymousUser_getResourcesWhenNoneExist_returnsEmptyList() throws Exception {

        when(resourceService.getResourcesByEvent(eventId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events/{eventId}/resources", eventId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(resourceService).getResourcesByEvent(eventId);
    }
}