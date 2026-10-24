
package com.malik.reservation.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreateAudit() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdateAudit() {
        updatedAt = Instant.now();
    }
}
