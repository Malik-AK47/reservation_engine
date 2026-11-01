
package com.malik.reservation.event.controller;

import com.malik.reservation.common.config.SecurityConfig;
import com.malik.reservation.event.dto.CreateEventRequest;
import com.malik.reservation.event.dto.EventResponse;
import com.malik.reservation.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.malik.reservation.event.entity.EventStatus;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({EventController.class, AdminEventController.class})
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousUser_canListPublishedEvents() throws Exception {
        when(eventService.getPublishedEvents(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUser_cannotCreateEvent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Event",
                                  "description": "Integration test",
                                  "location": "Pune",
                                  "startTime": "2027-01-01T10:00:00Z",
                                  "endTime": "2027-01-01T12:00:00Z",
                                  "cancellationCutoffMinutes": 30
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void anonymousUser_cannotPublishEvent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRole_cannotCreateEvent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Test Event",
                              "description": "Integration test",
                              "location": "Pune",
                              "startTime": "2027-01-01T10:00:00Z",
                              "endTime": "2027-01-01T12:00:00Z",
                              "cancellationCutoffMinutes": 30
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_canCreateEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-01T12:00:00Z");
        Instant now = Instant.now();

        EventResponse response = new EventResponse(
                eventId,
                "Test Event",
                "Integration test",
                "Pune",
                startTime,
                endTime,
                EventStatus.DRAFT,
                30,
                now,
                now
        );

        when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Test Event",
                              "description": "Integration test",
                              "location": "Pune",
                              "startTime": "2027-01-01T10:00:00Z",
                              "endTime": "2027-01-01T12:00:00Z",
                              "cancellationCutoffMinutes": 30
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/admin/events/" + eventId)))
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).createEvent(any(CreateEventRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_withInvalidRequestBody_receivesBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "",
                              "location": "Pune",
                              "startTime": "2027-01-01T10:00:00Z",
                              "endTime": "2027-01-01T12:00:00Z",
                              "cancellationCutoffMinutes": 30
                            }
                            """))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canListAllEvents() throws Exception {
        when(eventService.getAllEvents(eq(null), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isOk());

        verify(eventService).getAllEvents(eq(null), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canFilterEventsByStatus() throws Exception {
        when(eventService.getAllEvents(eq(EventStatus.DRAFT), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/events")
                        .param("status", "DRAFT")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(eventService).getAllEvents(eq(EventStatus.DRAFT), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_withInvalidStatus_shouldReceiveBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(eventService, never())
                .getAllEvents(any(), any(Pageable.class));
    }

    @Test
    void anonymousUser_cannotListAdminEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRole_cannotListAdminEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canRetrieveEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        EventResponse response = new EventResponse(
                eventId,
                "Draft Event",
                "Test description",
                "Pune",
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"),
                EventStatus.DRAFT,
                60,
                now,
                now
        );

        when(eventService.getEventById(eventId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.name").value("Draft Event"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).getEventById(eventId);
    }

    @Test
    void anonymousUser_cannotRetrieveAdminEvent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRole_cannotRetrieveAdminEvent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
