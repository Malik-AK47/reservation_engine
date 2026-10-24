
package com.malik.reservation.resource.entity;

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
}
