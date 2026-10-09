package com.traveleasy.bus.mapper;

import com.traveleasy.bus.dto.request.CreateScheduleRequest;
import com.traveleasy.bus.dto.request.UpdateScheduleRequest;
import com.traveleasy.bus.dto.response.ScheduleResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.BusOperator;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.AvailabilityStatus;
import com.traveleasy.bus.enums.ScheduleStatus;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ScheduleMapper {

    public Schedule toEntity(CreateScheduleRequest request, Bus bus, Route route, BusOperator operator) {
        if (request == null) {
            return null;
        }

        return Schedule.builder()
                .bus(bus)
                .route(route)
                .operator(operator)
                .scheduleDate(request.getScheduleDate())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .status(ScheduleStatus.SCHEDULED)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .seatInventory(new ArrayList<>())
                .fares(new ArrayList<>())
                .build();
    }

    public void updateEntityFromDto(UpdateScheduleRequest request, Schedule schedule, Bus bus, Route route) {
        if (request == null || schedule == null) {
            return;
        }

        if (bus != null) {
            schedule.setBus(bus);
        }
        if (route != null) {
            schedule.setRoute(route);
        }
        if (request.getScheduleDate() != null) {
            schedule.setScheduleDate(request.getScheduleDate());
        }
        if (request.getDepartureTime() != null) {
            schedule.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            schedule.setArrivalTime(request.getArrivalTime());
        }
        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
            if (request.getStatus() == ScheduleStatus.CANCELLED) {
                schedule.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
            }
        }
        if (request.getAvailabilityStatus() != null) {
            schedule.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        if (request.getCancellationReason() != null) {
            schedule.setCancellationReason(request.getCancellationReason().trim());
        }
    }

    public ScheduleResponse toResponse(Schedule schedule) {
        return toResponse(schedule, null);
    }

    public ScheduleResponse toResponse(Schedule schedule, Long availableSeatsCount) {
        if (schedule == null) {
            return null;
        }

        Long seatsCount = availableSeatsCount;
        if (seatsCount == null && schedule.getSeatInventory() != null && !schedule.getSeatInventory().isEmpty()) {
            seatsCount = schedule.getSeatInventory().stream()
                    .filter(inv -> inv.getStatus() == SeatInventoryStatus.AVAILABLE)
                    .count();
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .busId(schedule.getBus() != null ? schedule.getBus().getId() : null)
                .busNumber(schedule.getBus() != null ? schedule.getBus().getBusNumber() : null)
                .busRegistrationNumber(schedule.getBus() != null ? schedule.getBus().getRegistrationNumber() : null)
                .busType(schedule.getBus() != null ? schedule.getBus().getBusType() : null)
                .totalSeats(schedule.getBus() != null ? schedule.getBus().getTotalSeats() : null)
                .routeId(schedule.getRoute() != null ? schedule.getRoute().getId() : null)
                .routeCode(schedule.getRoute() != null ? schedule.getRoute().getRouteCode() : null)
                .origin(schedule.getRoute() != null ? schedule.getRoute().getOrigin() : null)
                .destination(schedule.getRoute() != null ? schedule.getRoute().getDestination() : null)
                .distanceKm(schedule.getRoute() != null ? schedule.getRoute().getDistanceKm() : null)
                .estimatedDurationMinutes(schedule.getRoute() != null ? schedule.getRoute().getEstimatedDurationMinutes() : null)
                .operatorId(schedule.getOperator() != null ? schedule.getOperator().getId() : null)
                .operatorCompanyName(schedule.getOperator() != null ? schedule.getOperator().getCompanyName() : null)
                .scheduleDate(schedule.getScheduleDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .status(schedule.getStatus())
                .availabilityStatus(schedule.getAvailabilityStatus())
                .cancellationReason(schedule.getCancellationReason())
                .availableSeatsCount(seatsCount)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
