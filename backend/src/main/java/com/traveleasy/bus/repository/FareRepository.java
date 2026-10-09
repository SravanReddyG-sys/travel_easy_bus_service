package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FareRepository extends JpaRepository<Fare, UUID> {
    List<Fare> findByScheduleId(UUID scheduleId);
    Optional<Fare> findByScheduleIdAndSeatType(UUID scheduleId, SeatType seatType);
    void deleteByScheduleId(UUID scheduleId);
}
