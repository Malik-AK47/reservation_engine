
package com.malik.reservation.event;

import com.malik.reservation.ReservationEngineApplication;
import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.Resource;
import com.malik.reservation.event.entity.ResourceStatus;
import com.malik.reservation.event.repo.EventRepository;
import com.malik.reservation.event.repo.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = ReservationEngineApplication.class)
class EventResourcePersistenceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("reservation_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    void shouldPersistEventAndItsResourceRelationship() {
        Event event = new Event("Concurrency Test Event", "Integration test",
                java.time.Instant.parse("2030-01-01T10:00:00Z"),
                java.time.Instant.parse("2030-01-01T12:00:00Z")
        );

        Event savedEvent = eventRepository.saveAndFlush(event);

        Resource resource = new Resource(savedEvent, "SEAT-001");

        Resource savedResource = resourceRepository.saveAndFlush(resource);

        assertThat(savedResource.getId()).isNotNull();
        assertThat(savedResource.getStatus())
                .isEqualTo(ResourceStatus.AVAILABLE);

        Resource reloaded = resourceRepository.findById(savedResource.getId())
                .orElseThrow();

        assertThat(reloaded.getEvent().getId())
                .isEqualTo(savedEvent.getId());

        assertThat(resourceRepository.findByEvent_Id(savedEvent.getId()))
                .extracting(Resource::getCode)
                .containsExactly("SEAT-001");
    }
}
