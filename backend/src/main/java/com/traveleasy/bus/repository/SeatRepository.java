package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByBusIdOrderByRowNumAscColNumAsc(UUID busId);
    List<Seat> findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(UUID busId);
    Optional<Seat> findByBusIdAndSeatNumber(UUID busId, String seatNumber);
    boolean existsByBusIdAndSeatNumber(UUID busId, String seatNumber);
    long countByBusId(UUID busId);
}
