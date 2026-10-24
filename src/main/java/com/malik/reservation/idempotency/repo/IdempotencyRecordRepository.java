package com.malik.reservation.idempotency.repo;

import com.malik.reservation.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByUser_IdAndOperationAndKey(UUID userId, String operation, String key);
}