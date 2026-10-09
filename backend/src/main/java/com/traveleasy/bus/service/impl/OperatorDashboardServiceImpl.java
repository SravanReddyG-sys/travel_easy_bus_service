package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.response.*;
import com.traveleasy.bus.entity.*;
import com.traveleasy.bus.enums.BusStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import com.traveleasy.bus.exception.ResourceNotFoundException;
import com.traveleasy.bus.mapper.ScheduleMapper;
import com.traveleasy.bus.repository.*;
import com.traveleasy.bus.security.SecurityUtils;
import com.traveleasy.bus.service.OperatorDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorDashboardServiceImpl implements OperatorDashboardService {

    private final BusOperatorRepository operatorRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatInventoryRepository inventoryRepository;
    private final FareRepository fareRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    @Transactional(readOnly = true)
    public OperatorDashboardSummaryResponse getDashboardSummary(UUID operatorId) {
        UUID effectiveOperatorId = resolveEffectiveOperatorId(operatorId);
        log.info("Generating dashboard summary for operator ID: {}", effectiveOperatorId);

        BusOperator operator = operatorRepository.findById(effectiveOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus operator not found with ID: " + effectiveOperatorId));

        // 1. Bus Fleet Stats
        List<Bus> buses = busRepository.findByOperatorId(effectiveOperatorId);
        long totalBuses = buses.size();
        long activeBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.ACTIVE).count();
        long maintenanceBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.MAINTENANCE).count();
        long inactiveBuses = buses.stream().filter(b -> b.getStatus() == BusStatus.INACTIVE).count();

        // 2. Route Stats
        List<Route> routes = routeRepository.findByOperatorId(effectiveOperatorId);
        long totalRoutes = routes.size();
        long activeRoutes = routes.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();

        // 3. Schedule Stats
        List<Schedule> schedules = scheduleRepository.findByOperatorId(effectiveOperatorId);
        LocalDate today = LocalDate.now();

        long totalSchedules = schedules.size();
        long todaySchedules = schedules.stream()
                .filter(s -> s.getScheduleDate() != null && s.getScheduleDate().equals(today))
                .count();
        long activeSchedules = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.SCHEDULED || s.getStatus() == ScheduleStatus.BOARDING)
                .count();
        long completedSchedules = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.COMPLETED)
                .count();
        long cancelledSchedules = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.CANCELLED)
                .count();

        // 4. Seat Capacity & Revenue Calculation
        long totalCapacity = 0;
        long totalBookedSeats = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Schedule schedule : schedules) {
            List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdWithSeats(schedule.getId());
            totalCapacity += inventories.size();

            Map<com.traveleasy.bus.enums.SeatType, BigDecimal> fareMap = loadFareMap(schedule);

            for (ScheduleSeatInventory inv : inventories) {
                if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                    totalBookedSeats++;
                    BigDecimal seatPrice = fareMap.getOrDefault(
                            inv.getSeat().getSeatType(),
                            fareMap.values().stream().findFirst().orElse(BigDecimal.ZERO)
                    );
                    totalRevenue = totalRevenue.add(seatPrice);
                }
            }
        }

        Double occupancyRate = totalCapacity > 0 ?
                Math.round(((double) totalBookedSeats / totalCapacity * 100.0) * 10.0) / 10.0 : 0.0;

        // 5. Recent schedules (most recent 5)
        List<ScheduleResponse> recentSchedules = schedules.stream()
                .sorted(Comparator.comparing(Schedule::getDepartureTime).reversed())
                .limit(5)
                .map(s -> {
                    long available = inventoryRepository.countAvailableSeatsByScheduleId(s.getId());
                    return scheduleMapper.toResponse(s, available);
                })
                .collect(Collectors.toList());

        return OperatorDashboardSummaryResponse.builder()
                .operatorId(operator.getId())
                .companyName(operator.getCompanyName())
                .totalBuses(totalBuses)
                .activeBuses(activeBuses)
                .maintenanceBuses(maintenanceBuses)
                .inactiveBuses(inactiveBuses)
                .totalRoutes(totalRoutes)
                .activeRoutes(activeRoutes)
                .totalSchedules(totalSchedules)
                .todaySchedules(todaySchedules)
                .activeSchedules(activeSchedules)
                .completedSchedules(completedSchedules)
                .cancelledSchedules(cancelledSchedules)
                .totalSeatsCapacity(totalCapacity)
                .totalSeatsBooked(totalBookedSeats)
                .occupancyRatePercentage(occupancyRate)
                .totalRevenue(totalRevenue)
                .currency("INR")
                .recentSchedules(recentSchedules)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatorRevenueStatResponse> getRevenueByRoute(UUID operatorId) {
        UUID effectiveOperatorId = resolveEffectiveOperatorId(operatorId);
        log.info("Calculating route revenue analytics for operator ID: {}", effectiveOperatorId);

        List<Route> routes = routeRepository.findByOperatorId(effectiveOperatorId);
        List<Schedule> schedules = scheduleRepository.findByOperatorId(effectiveOperatorId);

        Map<UUID, List<Schedule>> schedulesByRoute = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getRoute().getId()));

        List<OperatorRevenueStatResponse> result = new ArrayList<>();

        for (Route route : routes) {
            List<Schedule> routeSchedules = schedulesByRoute.getOrDefault(route.getId(), Collections.emptyList());
            long bookedSeatsCount = 0;
            BigDecimal routeRevenue = BigDecimal.ZERO;

            for (Schedule schedule : routeSchedules) {
                List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdWithSeats(schedule.getId());
                Map<com.traveleasy.bus.enums.SeatType, BigDecimal> fareMap = loadFareMap(schedule);

                for (ScheduleSeatInventory inv : inventories) {
                    if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                        bookedSeatsCount++;
                        BigDecimal price = fareMap.getOrDefault(
                                inv.getSeat().getSeatType(),
                                fareMap.values().stream().findFirst().orElse(BigDecimal.ZERO)
                        );
                        routeRevenue = routeRevenue.add(price);
                    }
                }
            }

            result.add(OperatorRevenueStatResponse.builder()
                    .routeId(route.getId())
                    .routeCode(route.getRouteCode())
                    .origin(route.getOrigin())
                    .destination(route.getDestination())
                    .schedulesCount(routeSchedules.size())
                    .totalBookedSeats(bookedSeatsCount)
                    .totalRevenue(routeRevenue)
                    .currency("INR")
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatorOccupancyStatResponse> getOccupancyAnalytics(UUID operatorId, LocalDate startDate, LocalDate endDate) {
        UUID effectiveOperatorId = resolveEffectiveOperatorId(operatorId);
        log.info("Calculating trip occupancy analytics for operator ID: {}", effectiveOperatorId);

        List<Schedule> schedules = scheduleRepository.findByOperatorId(effectiveOperatorId);

        return schedules.stream()
                .filter(s -> {
                    if (startDate != null && s.getScheduleDate().isBefore(startDate)) return false;
                    if (endDate != null && s.getScheduleDate().isAfter(endDate)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(Schedule::getDepartureTime).reversed())
                .map(s -> {
                    List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdWithSeats(s.getId());
                    int total = inventories.size();
                    long booked = inventories.stream().filter(inv -> inv.getStatus() == SeatInventoryStatus.BOOKED).count();
                    long available = inventories.stream().filter(inv -> inv.getStatus() == SeatInventoryStatus.AVAILABLE).count();
                    Double rate = total > 0 ? Math.round(((double) booked / total * 100.0) * 10.0) / 10.0 : 0.0;

                    return OperatorOccupancyStatResponse.builder()
                            .scheduleId(s.getId())
                            .busNumber(s.getBus().getBusNumber())
                            .origin(s.getRoute().getOrigin())
                            .destination(s.getRoute().getDestination())
                            .scheduleDate(s.getScheduleDate())
                            .departureTime(s.getDepartureTime())
                            .totalSeats(total)
                            .bookedSeats(booked)
                            .availableSeats(available)
                            .occupancyPercentage(rate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulePassengerManifestResponse getScheduleManifest(UUID scheduleId) {
        log.info("Fetching passenger manifest and seat roster for schedule ID: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + scheduleId));

        SecurityUtils.verifyOperatorOwnership(schedule.getOperator().getId());

        List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdWithSeats(scheduleId);
        Map<com.traveleasy.bus.enums.SeatType, BigDecimal> fareMap = loadFareMap(schedule);

        List<SeatReservationItemResponse> roster = new ArrayList<>();
        int bookedCount = 0;
        int lockedCount = 0;
        int availableCount = 0;
        int blockedCount = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO;

        for (ScheduleSeatInventory inv : inventories) {
            BigDecimal seatPrice = fareMap.getOrDefault(
                    inv.getSeat().getSeatType(),
                    fareMap.values().stream().findFirst().orElse(BigDecimal.ZERO)
            );

            if (inv.getStatus() == SeatInventoryStatus.BOOKED) {
                bookedCount++;
                grossRevenue = grossRevenue.add(seatPrice);
            } else if (inv.getStatus() == SeatInventoryStatus.LOCKED) {
                lockedCount++;
            } else if (inv.getStatus() == SeatInventoryStatus.AVAILABLE) {
                availableCount++;
            } else if (inv.getStatus() == SeatInventoryStatus.BLOCKED) {
                blockedCount++;
            }

            roster.add(SeatReservationItemResponse.builder()
                    .seatNumber(inv.getSeat().getSeatNumber())
                    .deck(inv.getSeat().getDeck())
                    .seatType(inv.getSeat().getSeatType())
                    .status(inv.getStatus())
                    .bookingReference(inv.getBookingReference())
                    .lockedBySession(inv.getLockedByBookingSession())
                    .lockExpiryTime(inv.getLockExpiryTime())
                    .seatFare(seatPrice)
                    .build());
        }

        int totalSeats = inventories.size();
        double occupancyRate = totalSeats > 0 ?
                Math.round(((double) bookedCount / totalSeats * 100.0) * 10.0) / 10.0 : 0.0;

        return SchedulePassengerManifestResponse.builder()
                .scheduleId(schedule.getId())
                .scheduleDate(schedule.getScheduleDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .status(schedule.getStatus())
                .availabilityStatus(schedule.getAvailabilityStatus())
                .busId(schedule.getBus().getId())
                .busNumber(schedule.getBus().getBusNumber())
                .registrationNumber(schedule.getBus().getRegistrationNumber())
                .busType(schedule.getBus().getBusType())
                .routeId(schedule.getRoute().getId())
                .routeCode(schedule.getRoute().getRouteCode())
                .origin(schedule.getRoute().getOrigin())
                .destination(schedule.getRoute().getDestination())
                .totalSeats(totalSeats)
                .bookedSeatsCount(bookedCount)
                .lockedSeatsCount(lockedCount)
                .availableSeatsCount(availableCount)
                .blockedSeatsCount(blockedCount)
                .occupancyPercentage(occupancyRate)
                .grossRevenue(grossRevenue)
                .currency("INR")
                .seatRoster(roster)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatorLiveBookingFeedResponse> getLiveBookingFeed(UUID operatorId, int limit) {
        UUID effectiveOperatorId = resolveEffectiveOperatorId(operatorId);
        log.info("Fetching live booking feed for operator ID: {}", effectiveOperatorId);

        List<Schedule> schedules = scheduleRepository.findByOperatorId(effectiveOperatorId);
        List<OperatorLiveBookingFeedResponse> feed = new ArrayList<>();
        int maxItems = limit > 0 ? limit : 20;

        for (Schedule schedule : schedules) {
            List<ScheduleSeatInventory> inventories = inventoryRepository.findByScheduleIdWithSeats(schedule.getId());
            Map<com.traveleasy.bus.enums.SeatType, BigDecimal> fareMap = loadFareMap(schedule);

            for (ScheduleSeatInventory inv : inventories) {
                if (inv.getStatus() == SeatInventoryStatus.BOOKED || inv.getStatus() == SeatInventoryStatus.LOCKED) {
                    BigDecimal seatPrice = fareMap.getOrDefault(
                            inv.getSeat().getSeatType(),
                            fareMap.values().stream().findFirst().orElse(BigDecimal.ZERO)
                    );

                    feed.add(OperatorLiveBookingFeedResponse.builder()
                            .scheduleId(schedule.getId())
                            .busNumber(schedule.getBus().getBusNumber())
                            .routeSummary(schedule.getRoute().getOrigin() + " -> " + schedule.getRoute().getDestination())
                            .seatNumber(inv.getSeat().getSeatNumber())
                            .status(inv.getStatus())
                            .bookingReference(inv.getBookingReference() != null ? inv.getBookingReference() : inv.getLockedByBookingSession())
                            .amount(seatPrice)
                            .eventTimestamp(inv.getUpdatedAt() != null ? inv.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : LocalDateTime.now())
                            .build());
                }
            }
        }

        return feed.stream()
                .sorted(Comparator.comparing(OperatorLiveBookingFeedResponse::getEventTimestamp).reversed())
                .limit(maxItems)
                .collect(Collectors.toList());
    }

    private UUID resolveEffectiveOperatorId(UUID requestedOperatorId) {
        if (requestedOperatorId != null) {
            SecurityUtils.verifyOperatorOwnership(requestedOperatorId);
            return requestedOperatorId;
        }
        return SecurityUtils.requireCurrentOperatorId();
    }

    private Map<com.traveleasy.bus.enums.SeatType, BigDecimal> loadFareMap(Schedule schedule) {
        Map<com.traveleasy.bus.enums.SeatType, BigDecimal> fareMap = new HashMap<>();
        List<Fare> fares = schedule.getFares() != null && !schedule.getFares().isEmpty() ?
                schedule.getFares() : fareRepository.findByScheduleId(schedule.getId());

        for (Fare f : fares) {
            if (f.getSeatType() != null && f.getTotalAmount() != null) {
                fareMap.put(f.getSeatType(), f.getTotalAmount());
            }
        }
        return fareMap;
    }
}
