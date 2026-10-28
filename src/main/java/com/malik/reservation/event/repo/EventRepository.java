
package com.malik.reservation.event.repo;

import com.malik.reservation.event.entity.Event;
import com.malik.reservation.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByIdAndStatus(UUID id, EventStatus status);

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);
}
