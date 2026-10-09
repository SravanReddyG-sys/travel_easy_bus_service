package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.BlockSeatRequest;
import com.traveleasy.bus.dto.request.BulkSeatConfigRequest;
import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.dto.response.PhysicalSeatResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatInventoryResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.enums.SeatType;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.DuplicateResourceException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.SeatMapper;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.repository.SeatRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScheduleSeatInventoryRepository scheduleSeatInventoryRepository;
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final FareRepository fareRepository;
    private final SeatMapper seatMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PhysicalSeatResponse> getBusSeats(UUID busId) {
        if (!busRepository.existsById(busId)) {
            throw new ResourceNotFoundException("Bus not found with ID: " + busId);
        }

        return seatRepository.findByBusIdOrderByRowNumAscColNumAsc(busId).stream()
                .map(seatMapper::toPhysicalResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PhysicalSeatResponse> configureBusSeats(UUID busId, BulkSeatConfigRequest request) {
        log.info("Configuring physical seats for bus ID: {}", busId);

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            return Collections.emptyList();
        }

        // Validate uniqueness of seat numbers in the request
        Set<String> seatNumbers = new HashSet<>();
        for (CreateSeatRequest seatReq : request.getSeats()) {
            String upperSeatNum = seatReq.getSeatNumber().trim().toUpperCase();
            if (!seatNumbers.add(upperSeatNum)) {
                throw new DuplicateResourceException("Seat", "seatNumber", upperSeatNum);
            }
        }

        List<Seat> savedSeats = new ArrayList<>();
        for (CreateSeatRequest seatReq : request.getSeats()) {
            String upperSeatNum = seatReq.getSeatNumber().trim().toUpperCase();
            Optional<Seat> existingSeatOpt = seatRepository.findByBusIdAndSeatNumber(busId, upperSeatNum);

            Seat seat;
            if (existingSeatOpt.isPresent()) {
                seat = existingSeatOpt.get();
                seat.setDeck(seatReq.getDeck());
                seat.setRowNum(seatReq.getRowNum());
                seat.setColNum(seatReq.getColNum());
                seat.setSeatType(seatReq.getSeatType());
                seat.setIsActive(true);
            } else {
                seat = seatMapper.toEntity(seatReq, bus);
            }
            savedSeats.add(seatRepository.save(seat));
        }

        bus.setTotalSeats((int) seatRepository.countByBusId(busId));
        busRepository.save(bus);

        log.info("Configured {} physical seats on bus ID: {}", savedSeats.size(), busId);
        return savedSeats.stream().map(seatMapper::toPhysicalResponse).collect(Collectors.toList());
    }

    @Override
    public PhysicalSeatResponse updateSeat(UUID busId, UUID seatId, CreateSeatRequest request) {
        log.info("Updating seat ID: {} on bus ID: {}", seatId, busId);

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with ID: " + seatId));

        if (!seat.getBus().getId().equals(busId)) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Seat " + seatId + " does not belong to bus " + busId);
        }

        String newSeatNum = request.getSeatNumber().trim().toUpperCase();
        if (!seat.getSeatNumber().equalsIgnoreCase(newSeatNum) && seatRepository.existsByBusIdAndSeatNumber(busId, newSeatNum)) {
            throw new DuplicateResourceException("Seat", "seatNumber", newSeatNum);
        }

        seat.setSeatNumber(newSeatNum);
        seat.setDeck(request.getDeck());
        seat.setRowNum(request.getRowNum());
        seat.setColNum(request.getColNum());
        seat.setSeatType(request.getSeatType());

        Seat updatedSeat = seatRepository.save(seat);
        return seatMapper.toPhysicalResponse(updatedSeat);
    }

    @Override
    public void deleteSeat(UUID busId, UUID seatId) {
        log.info("Deleting seat ID: {} on bus ID: {}", seatId, busId);

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with ID: " + seatId));

        if (!seat.getBus().getId().equals(busId)) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Seat " + seatId + " does not belong to bus " + busId);
        }

        seatRepository.delete(seat);
        bus.setTotalSeats((int) seatRepository.countByBusId(busId));
        busRepository.save(bus);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleSeatMapResponse getScheduleSeatMap(UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        List<ScheduleSeatInventory> inventories = scheduleSeatInventoryRepository.findByScheduleIdWithSeats(scheduleId);
        List<Fare> fares = fareRepository.findByScheduleId(scheduleId);

        Map<SeatType, Fare> fareMap = fares.stream()
                .collect(Collectors.toMap(Fare::getSeatType, f -> f, (f1, f2) -> f1));

        List<ScheduleSeatInventoryResponse> inventoryResponses = inventories.stream().map(inv -> {
            SeatType st = inv.getSeat() != null ? inv.getSeat().getSeatType() : SeatType.SEATER;
            Fare fare = fareMap.get(st);
            BigDecimal base = fare != null ? fare.getBaseAmount() : BigDecimal.ZERO;
            BigDecimal total = fare != null ? fare.getTotalAmount() : BigDecimal.ZERO;
            String currency = fare != null ? fare.getCurrency() : "INR";

            return seatMapper.toInventoryResponse(inv, base, total, currency);
        }).collect(Collectors.toList());

        return seatMapper.toSeatMapResponse(schedule, inventoryResponses);
    }

    @Override
    public ScheduleSeatInventoryResponse blockOrUnblockSeat(UUID scheduleId, String seatNumber, BlockSeatRequest request) {
        log.info("Block/Unblock seat: {} on schedule: {}, blocked={}", seatNumber, scheduleId, request.getBlocked());

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        ScheduleSeatInventory inventory = scheduleSeatInventoryRepository
                .findByScheduleIdAndSeatNumber(scheduleId, seatNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seat " + seatNumber + " not found in schedule " + scheduleId + " inventory"));

        if (Boolean.TRUE.equals(request.getBlocked())) {
            if (inventory.getStatus() == SeatInventoryStatus.BOOKED) {
                throw new BusDomainException(HttpStatus.CONFLICT, ErrorCode.SEAT_NOT_AVAILABLE,
                        "Cannot block seat " + seatNumber + " because it is already BOOKED");
            }
            if (inventory.getStatus() == SeatInventoryStatus.LOCKED) {
                throw new BusDomainException(HttpStatus.CONFLICT, ErrorCode.SEAT_ALREADY_LOCKED,
                        "Cannot block seat " + seatNumber + " because it is currently LOCKED by a customer");
            }
            inventory.setStatus(SeatInventoryStatus.BLOCKED);
        } else {
            inventory.setStatus(SeatInventoryStatus.AVAILABLE);
            inventory.setLockExpiryTime(null);
            inventory.setLockedByBookingSession(null);
        }

        ScheduleSeatInventory savedInventory = scheduleSeatInventoryRepository.save(inventory);

        Fare fare = fareRepository.findByScheduleIdAndSeatType(scheduleId, savedInventory.getSeat().getSeatType())
                .orElse(null);
        BigDecimal base = fare != null ? fare.getBaseAmount() : BigDecimal.ZERO;
        BigDecimal total = fare != null ? fare.getTotalAmount() : BigDecimal.ZERO;
        String currency = fare != null ? fare.getCurrency() : "INR";

        return seatMapper.toInventoryResponse(savedInventory, base, total, currency);
    }
}
