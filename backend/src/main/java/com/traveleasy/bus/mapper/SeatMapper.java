package com.traveleasy.bus.mapper;

import com.traveleasy.bus.dto.request.CreateSeatRequest;
import com.traveleasy.bus.dto.response.PhysicalSeatResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatInventoryResponse;
import com.traveleasy.bus.dto.response.ScheduleSeatMapResponse;
import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.entity.ScheduleSeatInventory;
import com.traveleasy.bus.entity.Seat;
import com.traveleasy.bus.enums.Deck;
import com.traveleasy.bus.enums.SeatInventoryStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeatMapper {

    public Seat toEntity(CreateSeatRequest request, Bus bus) {
        if (request == null) {
            return null;
        }

        return Seat.builder()
                .bus(bus)
                .seatNumber(request.getSeatNumber().trim().toUpperCase())
                .deck(request.getDeck() != null ? request.getDeck() : Deck.LOWER)
                .rowNum(request.getRowNum())
                .colNum(request.getColNum())
                .seatType(request.getSeatType())
                .isActive(true)
                .build();
    }

    public PhysicalSeatResponse toPhysicalResponse(Seat seat) {
        if (seat == null) {
            return null;
        }

        return PhysicalSeatResponse.builder()
                .id(seat.getId())
                .busId(seat.getBus() != null ? seat.getBus().getId() : null)
                .seatNumber(seat.getSeatNumber())
                .deck(seat.getDeck())
                .rowNum(seat.getRowNum())
                .colNum(seat.getColNum())
                .seatType(seat.getSeatType())
                .isActive(seat.getIsActive())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }

    public ScheduleSeatInventoryResponse toInventoryResponse(
            ScheduleSeatInventory inventory,
            BigDecimal basePrice,
            BigDecimal totalPrice,
            String currency) {

        if (inventory == null) {
            return null;
        }

        Seat seat = inventory.getSeat();
        boolean isAvailable = inventory.getStatus() == SeatInventoryStatus.AVAILABLE;

        // If locked but lock expired, consider available
        if (inventory.getStatus() == SeatInventoryStatus.LOCKED && inventory.getLockExpiryTime() != null
                && inventory.getLockExpiryTime().isBefore(LocalDateTime.now())) {
            isAvailable = true;
        }

        return ScheduleSeatInventoryResponse.builder()
                .inventoryId(inventory.getId())
                .seatId(seat != null ? seat.getId() : null)
                .seatNumber(seat != null ? seat.getSeatNumber() : null)
                .deck(seat != null ? seat.getDeck() : Deck.LOWER)
                .rowNum(seat != null ? seat.getRowNum() : 0)
                .colNum(seat != null ? seat.getColNum() : 0)
                .seatType(seat != null ? seat.getSeatType() : null)
                .status(inventory.getStatus())
                .isAvailable(isAvailable)
                .basePrice(basePrice)
                .totalPrice(totalPrice)
                .currency(currency != null ? currency : "INR")
                .lockExpiryTime(inventory.getLockExpiryTime())
                .build();
    }

    public ScheduleSeatMapResponse toSeatMapResponse(Schedule schedule, List<ScheduleSeatInventoryResponse> inventoryResponses) {
        if (schedule == null) {
            return null;
        }

        List<ScheduleSeatInventoryResponse> lower = new ArrayList<>();
        List<ScheduleSeatInventoryResponse> upper = new ArrayList<>();
        long availableCount = 0;

        if (inventoryResponses != null) {
            for (ScheduleSeatInventoryResponse item : inventoryResponses) {
                if (Boolean.TRUE.equals(item.getIsAvailable())) {
                    availableCount++;
                }
                if (item.getDeck() == Deck.UPPER) {
                    upper.add(item);
                } else {
                    lower.add(item);
                }
            }
        }

        return ScheduleSeatMapResponse.builder()
                .scheduleId(schedule.getId())
                .busId(schedule.getBus() != null ? schedule.getBus().getId() : null)
                .busNumber(schedule.getBus() != null ? schedule.getBus().getBusNumber() : null)
                .registrationNumber(schedule.getBus() != null ? schedule.getBus().getRegistrationNumber() : null)
                .totalSeats(schedule.getBus() != null ? schedule.getBus().getTotalSeats() : null)
                .availableSeatsCount(availableCount)
                .lowerDeckSeats(lower)
                .upperDeckSeats(upper)
                .build();
    }
}
