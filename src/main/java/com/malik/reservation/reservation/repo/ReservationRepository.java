
package com.malik.reservation.reservation.repo;

import com.malik.reservation.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReservationRepository
        extends JpaRepository<Reservation, UUID> {
}
