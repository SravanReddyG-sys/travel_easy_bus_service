package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.BusAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusAvailabilityRepository extends JpaRepository<BusAvailability, UUID> {
    List<BusAvailability> findByScheduleIdOrderByEffectiveFromDesc(UUID scheduleId);
    void deleteByScheduleId(UUID scheduleId);
}
