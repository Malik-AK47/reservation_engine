
package com.malik.reservation.resource.controller;

import com.malik.reservation.common.config.SecurityConfig;
import com.malik.reservation.resource.dto.CreateResourceRequest;
import com.malik.reservation.resource.dto.ResourceResponse;
import com.malik.reservation.resource.entity.ResourceStatus;
import com.malik.reservation.resource.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers.*;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminResourceController.class)
@Import(SecurityConfig.class)
class AdminResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousUser_cannotCreateResource() throws Exception {

        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/resources", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceCode": "SEAT-A-01"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(resourceService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRole_cannotCreateResource() throws Exception {

        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/resources", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceCode": "SEAT-A-01"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resourceService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_canCreateResource() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();

        ResourceResponse response = new ResourceResponse(resourceId, eventId, "SEAT-A-01", ResourceStatus.AVAILABLE, now, now);

        when(resourceService.createResource(eq(eventId), any(CreateResourceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/resources", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceCode": "SEAT-A-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "http://localhost/api/v1/admin/events/" + eventId + "/resources/" + resourceId))
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.resourceCode").value("SEAT-A-01"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(resourceService).createResource(eq(eventId), any(CreateResourceRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_withBlankResourceCode_receivesBadRequest() throws Exception {

        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/resources", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceCode": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resourceService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRole_withMissingResourceCode_receivesBadRequest() throws Exception {

        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/resources", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resourceService);
    }
}
