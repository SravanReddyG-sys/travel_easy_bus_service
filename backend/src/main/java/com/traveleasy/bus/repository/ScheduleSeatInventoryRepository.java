package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleSeatInventoryRepository extends JpaRepository<ScheduleSeatInventory, UUID> {

    @Query("SELECT ssi FROM ScheduleSeatInventory ssi " +
           "JOIN FETCH ssi.seat st " +
           "WHERE ssi.schedule.id = :scheduleId " +
           "ORDER BY st.deck ASC, st.rowNum ASC, st.colNum ASC")
    List<ScheduleSeatInventory> findByScheduleIdWithSeats(@Param("scheduleId") UUID scheduleId);

    Optional<ScheduleSeatInventory> findByScheduleIdAndSeatId(UUID scheduleId, UUID seatId);

    @Query("SELECT ssi FROM ScheduleSeatInventory ssi " +
           "JOIN FETCH ssi.seat st " +
           "WHERE ssi.schedule.id = :scheduleId AND st.seatNumber = :seatNumber")
    Optional<ScheduleSeatInventory> findByScheduleIdAndSeatNumber(
            @Param("scheduleId") UUID scheduleId,
            @Param("seatNumber") String seatNumber);

    /**
     * Critical Concurrency Lock Query:
     * Acquires DB row-level pessimistic write lock (SELECT ... FOR UPDATE) on the requested seats.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ssi FROM ScheduleSeatInventory ssi " +
           "JOIN FETCH ssi.seat st " +
           "WHERE ssi.schedule.id = :scheduleId AND st.seatNumber IN :seatNumbers")
    List<ScheduleSeatInventory> findByScheduleIdAndSeatNumbersForUpdate(
            @Param("scheduleId") UUID scheduleId,
            @Param("seatNumbers") List<String> seatNumbers);

    @Query("SELECT COUNT(ssi) FROM ScheduleSeatInventory ssi " +
           "WHERE ssi.schedule.id = :scheduleId AND ssi.status = 'AVAILABLE'")
    long countAvailableSeatsByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT ssi FROM ScheduleSeatInventory ssi " +
           "WHERE ssi.status = 'LOCKED' AND ssi.lockExpiryTime < :now")
    List<ScheduleSeatInventory> findExpiredLocks(@Param("now") LocalDateTime now);

    List<ScheduleSeatInventory> findByScheduleIdAndBookingReference(UUID scheduleId, String bookingReference);

    List<ScheduleSeatInventory> findByBookingReference(String bookingReference);
}
