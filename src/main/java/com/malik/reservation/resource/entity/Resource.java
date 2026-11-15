
package com.malik.reservation.resource.entity;

import com.malik.reservation.common.exception.BusinessException;
import com.malik.reservation.common.persistence.AuditableEntity;
import com.malik.reservation.event.entity.Event;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "resource_code", nullable = false, length = 100)
    private String resourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ResourceStatus status;

    public static Resource create(Event event, String resourceCode) {
        Resource resource = new Resource();
        resource.setEvent(event);
        resource.setResourceCode(resourceCode);
        resource.setStatus(ResourceStatus.AVAILABLE);
        return resource;
    }

    public void hold() {
        ensureStatus(ResourceStatus.AVAILABLE);
        this.status = ResourceStatus.HELD;
    }

    public void book() {
        ensureStatus(ResourceStatus.HELD);
        this.status = ResourceStatus.BOOKED;
    }

    public void release() {
        if (status != ResourceStatus.HELD && status != ResourceStatus.BOOKED) {
            throw new BusinessException("Only held or booked resources can be released");
        }

        this.status = ResourceStatus.AVAILABLE;
    }

    private void ensureStatus(ResourceStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new BusinessException("Resource must be " + expectedStatus + " but is " + status);
        }
    }
}
