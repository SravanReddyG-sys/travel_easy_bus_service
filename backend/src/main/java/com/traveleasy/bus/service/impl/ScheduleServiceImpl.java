package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.dto.response.*;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.exception.BusDomainException;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.exception.InvalidScheduleException;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.BusRepository;
import com.traveleasy.bus.repository.RouteRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.repository.SeatRepository;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.service.FareService;
import com.traveleasy.bus.service.ScheduleService;
import com.traveleasy.bus.service.SeatService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatInventoryRepository scheduleSeatInventoryRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;
    private final ScheduleMapper scheduleMapper;
    private final SeatService seatService;
    private final FareService fareService;
    private final RouteMapper routeMapper;
    private final com.traveleasy.bus.service.EventOutboxService eventOutboxService;

    @Override
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        log.info("Creating schedule for busId: {}, routeId: {}, date: {}",
                request.getBusId(), request.getRouteId(), request.getScheduleDate());

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + request.getBusId()));

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + request.getRouteId()));

        SecurityUtils.verifyOperatorOwnership(bus.getOperator().getId());
        SecurityUtils.verifyOperatorOwnership(route.getOperator().getId());

        if (!bus.getOperator().getId().equals(route.getOperator().getId())) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                    "Bus and Route must belong to the same operator");
        }

        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.BUS_NOT_ACTIVE,
                    "Cannot schedule a bus that is not in ACTIVE status (current status: " + bus.getStatus() + ")");
        }

        if (!Boolean.TRUE.equals(route.getIsActive())) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.ROUTE_INVALID,
                    "Cannot create schedule for an inactive route");
        }

        validateScheduleTiming(request.getScheduleDate(), request.getDepartureTime(), request.getArrivalTime());

        boolean overlapping = scheduleRepository.hasOverlappingSchedule(
                bus.getId(),
                UUID.randomUUID(),
                request.getDepartureTime(),
                request.getArrivalTime()
        );

        if (overlapping) {
            throw new InvalidScheduleException("Bus " + bus.getRegistrationNumber() +
                    " already has an active overlapping schedule in this timeframe");
        }

        Schedule schedule = scheduleMapper.toEntity(request, bus, route, bus.getOperator());

        List<Seat> physicalSeats = seatRepository.findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(bus.getId());
        if (!physicalSeats.isEmpty()) {
            for (Seat seat : physicalSeats) {
                ScheduleSeatInventory inv = ScheduleSeatInventory.builder()
                        .schedule(schedule)
                        .seat(seat)
                        .status(SeatInventoryStatus.AVAILABLE)
                        .build();
                schedule.getSeatInventory().add(inv);
            }
        }

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Created schedule ID: {} with {} seat inventory items", savedSchedule.getId(), savedSchedule.getSeatInventory().size());

        eventOutboxService.saveEvent(com.traveleasy.bus.event.ScheduleStatusChangedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(savedSchedule.getId())
                .eventType("SCHEDULE_CREATED")
                .scheduleId(savedSchedule.getId())
                .status(savedSchedule.getStatus())
                .availabilityStatus(savedSchedule.getAvailabilityStatus())
                .build());

        return scheduleMapper.toResponse(savedSchedule, (long) savedSchedule.getSeatInventory().size());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        long availableSeatsCount = scheduleSeatInventoryRepository.countAvailableSeatsByScheduleId(schedule.getId());
        return scheduleMapper.toResponse(schedule, availableSeatsCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> getSchedules(
            UUID operatorId,
            UUID routeId,
            UUID busId,
            LocalDate scheduleDate,
            ScheduleStatus status,
            AvailabilityStatus availabilityStatus,
            Pageable pageable) {

        Specification<Schedule> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operator").get("id"), operatorId));
            }
            if (routeId != null) {
                predicates.add(cb.equal(root.get("route").get("id"), routeId));
            }
            if (busId != null) {
                predicates.add(cb.equal(root.get("bus").get("id"), busId));
            }
            if (scheduleDate != null) {
                predicates.add(cb.equal(root.get("scheduleDate"), scheduleDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (availabilityStatus != null) {
                predicates.add(cb.equal(root.get("availabilityStatus"), availabilityStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Schedule> page = scheduleRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(scheduleMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> getOperatorSchedules(Pageable pageable) {
        UUID operatorId = SecurityUtils.requireCurrentOperatorId();
        Page<Schedule> page = scheduleRepository.findByOperatorId(operatorId, pageable);
        return PageResponse.from(page.map(scheduleMapper::toResponse));
    }

    @Override
    public ScheduleResponse updateSchedule(UUID scheduleId, UpdateScheduleRequest request) {
        log.info("Updating schedule ID: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot modify a schedule that has already been COMPLETED");
        }

        Bus targetBus = schedule.getBus();
        if (request.getBusId() != null && !request.getBusId().equals(targetBus.getId())) {
            targetBus = busRepository.findById(request.getBusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + request.getBusId()));
            SecurityUtils.verifyOperatorOwnership(targetBus.getOperator().getId());
            if (targetBus.getStatus() != BusStatus.ACTIVE) {
                throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.BUS_NOT_ACTIVE,
                        "Assigned bus must be in ACTIVE status");
            }
        }

        Route targetRoute = schedule.getRoute();
        if (request.getRouteId() != null && !request.getRouteId().equals(targetRoute.getId())) {
            targetRoute = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + request.getRouteId()));
            SecurityUtils.verifyOperatorOwnership(targetRoute.getOperator().getId());
            if (!Boolean.TRUE.equals(targetRoute.getIsActive())) {
                throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.ROUTE_INVALID,
                        "Assigned route must be active");
            }
        }

        LocalDateTime departure = request.getDepartureTime() != null ? request.getDepartureTime() : schedule.getDepartureTime();
        LocalDateTime arrival = request.getArrivalTime() != null ? request.getArrivalTime() : schedule.getArrivalTime();
        LocalDate date = request.getScheduleDate() != null ? request.getScheduleDate() : schedule.getScheduleDate();

        validateScheduleTiming(date, departure, arrival);

        boolean overlapping = scheduleRepository.hasOverlappingSchedule(
                targetBus.getId(),
                schedule.getId(),
                departure,
                arrival
        );

        if (overlapping) {
            throw new InvalidScheduleException("Bus already has an active overlapping schedule in this timeframe");
        }

        scheduleMapper.updateEntityFromDto(request, schedule, targetBus, targetRoute);
        Schedule updatedSchedule = scheduleRepository.save(schedule);

        long availableSeatsCount = scheduleSeatInventoryRepository.countAvailableSeatsByScheduleId(updatedSchedule.getId());
        return scheduleMapper.toResponse(updatedSchedule, availableSeatsCount);
    }

    @Override
    public ScheduleResponse cancelSchedule(UUID scheduleId, String cancellationReason) {
        log.info("Cancelling schedule ID: {} with reason: {}", scheduleId, cancellationReason);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new BusDomainException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot cancel a schedule that is already COMPLETED");
        }

        schedule.setStatus(ScheduleStatus.CANCELLED);
        schedule.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        schedule.setCancellationReason(cancellationReason != null ? cancellationReason.trim() : "Cancelled by operator");

        Schedule cancelledSchedule = scheduleRepository.save(schedule);

        eventOutboxService.saveEvent(com.traveleasy.bus.event.ScheduleStatusChangedEvent.builder()
                .aggregateType("SCHEDULE")
                .aggregateId(cancelledSchedule.getId())
                .eventType("SCHEDULE_CANCELLED")
                .scheduleId(cancelledSchedule.getId())
                .status(cancelledSchedule.getStatus())
                .availabilityStatus(cancelledSchedule.getAvailabilityStatus())
                .reason(cancellationReason)
                .build());

        long availableSeatsCount = scheduleSeatInventoryRepository.countAvailableSeatsByScheduleId(cancelledSchedule.getId());
        return scheduleMapper.toResponse(cancelledSchedule, availableSeatsCount);
    }

    @Override
    public void deleteSchedule(UUID scheduleId) {
        log.info("Deleting schedule ID: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        scheduleRepository.delete(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public TravellerScheduleDetailResponse getTravellerScheduleDetails(UUID scheduleId) {
        log.info("Fetching traveller schedule details for ID: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        Bus bus = schedule.getBus();
        Route route = schedule.getRoute();
        BusOperator operator = schedule.getOperator();

        // 1. Get Live Seat Map
        ScheduleSeatMapResponse seatMap = seatService.getScheduleSeatMap(scheduleId);

        // 2. Get Fares
        List<FareResponse> fares = fareService.getFaresByScheduleId(scheduleId);

        // 3. Map Route Stops & Boarding / Dropping Points
        List<RouteStopResponse> stops = route.getStops() != null ?
                route.getStops().stream().map(routeMapper::toStopResponse).collect(Collectors.toList()) :
                Collections.emptyList();

        List<BoardingPointResponse> boardingPoints = route.getBoardingPoints() != null ?
                route.getBoardingPoints().stream().map(routeMapper::toBoardingResponse).collect(Collectors.toList()) :
                Collections.emptyList();

        List<DroppingPointResponse> droppingPoints = route.getDroppingPoints() != null ?
                route.getDroppingPoints().stream().map(routeMapper::toDroppingResponse).collect(Collectors.toList()) :
                Collections.emptyList();

        int durationMinutes = route.getEstimatedDurationMinutes() != null ?
                route.getEstimatedDurationMinutes() :
                (int) Duration.between(schedule.getDepartureTime(), schedule.getArrivalTime()).toMinutes();

        return TravellerScheduleDetailResponse.builder()
                .scheduleId(schedule.getId())
                .scheduleDate(schedule.getScheduleDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .durationMinutes(durationMinutes)
                .status(schedule.getStatus())
                .availabilityStatus(schedule.getAvailabilityStatus())
                .cancellationReason(schedule.getCancellationReason())
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .registrationNumber(bus.getRegistrationNumber())
                .busType(bus.getBusType())
                .totalSeats(bus.getTotalSeats())
                .amenities(bus.getAmenities() != null ? new ArrayList<>(bus.getAmenities()) : Collections.emptyList())
                .operatorId(operator.getId())
                .operatorCompanyName(operator.getCompanyName())
                .operatorCode(operator.getOperatorCode())
                .contactPhone(operator.getContactPhone())
                .contactEmail(operator.getContactEmail())
                .operatorRating(4.5)
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .distanceKm(route.getDistanceKm())
                .stops(stops)
                .boardingPoints(boardingPoints)
                .droppingPoints(droppingPoints)
                .fares(fares)
                .seatMap(seatMap)
                .build();
    }

    private void validateScheduleTiming(LocalDate scheduleDate, LocalDateTime departureTime, LocalDateTime arrivalTime) {
        if (departureTime.isAfter(arrivalTime) || departureTime.isEqual(arrivalTime)) {
            throw new InvalidScheduleException("Departure time must be strictly before arrival time");
        }

        if (!scheduleDate.equals(departureTime.toLocalDate())) {
            throw new InvalidScheduleException("Schedule date (" + scheduleDate +
                    ") must match the departure date (" + departureTime.toLocalDate() + ")");
        }
    }
}
