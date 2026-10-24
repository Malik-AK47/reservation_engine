package com.malik.reservation.resource.repo;

import com.malik.reservation.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByEvent_Id(UUID eventId);
}