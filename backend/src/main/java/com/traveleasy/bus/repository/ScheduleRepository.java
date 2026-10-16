package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID>, JpaSpecificationExecutor<Schedule> {

    List<Schedule> findByOperatorId(UUID operatorId);
    Page<Schedule> findByOperatorId(UUID operatorId, Pageable pageable);
    Optional<Schedule> findByIdAndOperatorId(UUID id, UUID operatorId);

    List<Schedule> findByRouteIdAndScheduleDate(UUID routeId, LocalDate scheduleDate);
    List<Schedule> findByBusIdAndScheduleDate(UUID busId, LocalDate scheduleDate);

    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.bus b " +
           "JOIN FETCH s.route r " +
           "JOIN FETCH s.operator o " +
           "WHERE LOWER(r.origin) = LOWER(:origin) " +
           "AND LOWER(r.destination) = LOWER(:destination) " +
           "AND s.scheduleDate = :scheduleDate " +
           "AND s.status = 'SCHEDULED' " +
           "AND s.availabilityStatus = 'AVAILABLE'")
    List<Schedule> findAvailableSchedulesForSearch(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("scheduleDate") LocalDate scheduleDate);

    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.bus b " +
           "JOIN FETCH s.route r " +
           "JOIN FETCH s.operator o " +
           "WHERE LOWER(r.origin) = LOWER(:origin) " +
           "AND LOWER(r.destination) = LOWER(:destination) " +
           "AND s.status = 'SCHEDULED' " +
           "ORDER BY s.departureTime ASC")
    List<Schedule> findTemplateSchedulesForRoute(
            @Param("origin") String origin,
            @Param("destination") String destination);

    @Query("SELECT COUNT(s) > 0 FROM Schedule s " +
           "WHERE s.bus.id = :busId " +
           "AND s.id <> :excludeScheduleId " +
           "AND s.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "AND ((s.departureTime < :arrivalTime AND s.arrivalTime > :departureTime))")
    boolean hasOverlappingSchedule(
            @Param("busId") UUID busId,
            @Param("excludeScheduleId") UUID excludeScheduleId,
            @Param("departureTime") LocalDateTime departureTime,
            @Param("arrivalTime") LocalDateTime arrivalTime);
}
