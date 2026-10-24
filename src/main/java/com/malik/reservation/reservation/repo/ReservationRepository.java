package com.malik.reservation.reservation.repo;

import com.malik.reservation.reservation.entity.Reservation;
import com.malik.reservation.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByResource_IdAndStatusIn(UUID resourceId, Collection<ReservationStatus> statuses);
}