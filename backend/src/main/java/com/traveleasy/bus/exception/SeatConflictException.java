package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class SeatConflictException extends BusDomainException {

    public SeatConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.SEAT_ALREADY_LOCKED, message);
    }

    public SeatConflictException(ErrorCode errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public static SeatConflictException alreadyBooked(String seatNumber) {
        return new SeatConflictException(ErrorCode.SEAT_NOT_AVAILABLE, "Seat " + seatNumber + " is already booked");
    }

    public static SeatConflictException blocked(String seatNumber) {
        return new SeatConflictException(ErrorCode.SEAT_BLOCKED, "Seat " + seatNumber + " is blocked for crew/administrative use");
    }

    public static SeatConflictException alreadyLocked(String seatNumber, LocalDateTime expiry) {
        return new SeatConflictException(ErrorCode.SEAT_ALREADY_LOCKED,
                "Seat " + seatNumber + " is currently locked by another session until " + expiry);
    }
}
