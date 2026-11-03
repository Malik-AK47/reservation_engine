
package com.malik.reservation.resource.service;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.exception.InvalidRequestException;
import com.malik.reservation.common.exception.ResourceNotFoundException;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.EventStatus;
import com.malik.reservation.event.repo.EventRepository;
import com.malik.reservation.resource.dto.CreateResourceRequest;
import com.malik.reservation.resource.dto.ResourceResponse;
import com.malik.reservation.resource.entity.Resource;
import com.malik.reservation.resource.entity.ResourceStatus;
import com.malik.reservation.resource.repo.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ResourceService resourceService;

    private final UUID eventId = UUID.randomUUID();

    private Event createDraftEvent() {
        return Event.createDraft(
                "Test Event",
                "Test Description",
                "Akola",
                Instant.parse("2027-01-01T10:00:00Z"),
                Instant.parse("2027-01-01T12:00:00Z"),
                30
        );
    }

    private Event createPublishedEvent() {
        Event event = createDraftEvent();
        event.publish();
        return event;
    }

    @Test
    void createResource_shouldCreateAvailableResource_whenEventIsDraft() {
        Event event = createDraftEvent();

        when(eventRepository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        when(resourceRepository.saveAndFlush(any(Resource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.createResource(eventId, new CreateResourceRequest(" seat-a "));

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        verify(resourceRepository).saveAndFlush(resourceCaptor.capture());

        Resource savedResource = resourceCaptor.getValue();

        assertEquals("SEAT-A", savedResource.getResourceCode());
        assertEquals(ResourceStatus.AVAILABLE, savedResource.getStatus());
        assertSame(event, savedResource.getEvent());

        assertEquals("SEAT-A", response.resourceCode());
        assertEquals(ResourceStatus.AVAILABLE, response.status());

        verify(eventRepository).findByIdForUpdate(eventId);
    }

    @Test
    void createResource_shouldNormalizeResourceCode() {
        Event event = createDraftEvent();

        when(eventRepository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        when(resourceRepository.saveAndFlush(any(Resource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.createResource(eventId, new CreateResourceRequest("  seat-a-01  "));

        assertEquals("SEAT-A-01", response.resourceCode());
    }

    @Test
    void createResource_shouldThrowNotFound_whenEventDoesNotExist() {
        when(eventRepository.findByIdForUpdate(eventId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest("SEAT-A")));

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void createResource_shouldThrowBusinessException_whenEventIsPublished() {
        Event event = createPublishedEvent();

        when(eventRepository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(BusinessException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest("SEAT-A")));

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void createResource_shouldThrowBusinessException_whenEventIsCancelled() {
        Event event = createPublishedEvent();
        event.cancel();

        when(eventRepository.findByIdForUpdate(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(BusinessException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest("SEAT-A")));

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void createResource_shouldRejectBlankResourceCode() {
        assertThrows(InvalidRequestException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest("   ")));

        verifyNoInteractions(eventRepository, resourceRepository);
    }

    @Test
    void createResource_shouldRejectNullResourceCode() {
        assertThrows(InvalidRequestException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest(null)));

        verifyNoInteractions(eventRepository, resourceRepository);
    }

    @Test
    void createResource_shouldRejectCodeLongerThan100CharactersAfterNormalization() {
        String resourceCode = "A".repeat(101);

        assertThrows(InvalidRequestException.class,
                () -> resourceService.createResource(eventId, new CreateResourceRequest(resourceCode)));

        verifyNoInteractions(eventRepository, resourceRepository);
    }

    @Test
    void getResourcesByEvent_shouldReturnResources_whenEventIsPublished() {
        Event event = createPublishedEvent();

        Resource resource1 = Resource.create(event, "SEAT-A");
        Resource resource2 = Resource.create(event, "SEAT-B");

        when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
                .thenReturn(Optional.of(event));

        when(resourceRepository.findByEvent_Id(eventId))
                .thenReturn(List.of(resource1, resource2));

        List<ResourceResponse> responses = resourceService.getResourcesByEvent(eventId);

        assertEquals(2, responses.size());
        assertEquals("SEAT-A", responses.get(0).resourceCode());
        assertEquals("SEAT-B", responses.get(1).resourceCode());

        assertEquals(ResourceStatus.AVAILABLE, responses.get(0).status());
        assertEquals(ResourceStatus.AVAILABLE, responses.get(1).status());

        verify(eventRepository).findByIdAndStatus(eventId, EventStatus.PUBLISHED);

        verify(resourceRepository).findByEvent_Id(eventId);
    }

    @Test
    void getResourcesByEvent_shouldReturnEmptyList_whenEventHasNoResources() {
        Event event = createPublishedEvent();

        when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
                .thenReturn(Optional.of(event));

        when(resourceRepository.findByEvent_Id(eventId))
                .thenReturn(List.of());

        List<ResourceResponse> responses = resourceService.getResourcesByEvent(eventId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void getResourcesByEvent_shouldThrowNotFound_whenEventIsNotPublishedOrDoesNotExist() {
        when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resourceService.getResourcesByEvent(eventId));

        verifyNoInteractions(resourceRepository);
    }
}
