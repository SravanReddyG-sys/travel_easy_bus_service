package com.traveleasy.bus.exception;

public enum ErrorCode {
    // Generic Errors
    INTERNAL_SERVER_ERROR("BUS_500", "Internal server error occurred"),
    INVALID_REQUEST("BUS_400", "Invalid request format or parameters"),
    VALIDATION_ERROR("BUS_VALIDATION_ERROR", "Request validation failed"),
    RESOURCE_NOT_FOUND("BUS_NOT_FOUND", "Requested resource was not found"),
    DUPLICATE_RESOURCE("BUS_DUPLICATE_RESOURCE", "Resource already exists"),

    // Security & Authorization Errors
    UNAUTHORIZED("BUS_UNAUTHORIZED", "Authentication required or invalid token"),
    FORBIDDEN("BUS_FORBIDDEN", "You do not have permission to access or modify this resource"),
    OPERATOR_OWNERSHIP_MISMATCH("BUS_OWNERSHIP_MISMATCH", "Authenticated operator does not own this resource"),

    // Bus & Schedule Domain Errors
    BUS_NOT_ACTIVE("BUS_INACTIVE", "Bus is inactive or under maintenance"),
    ROUTE_INVALID("BUS_ROUTE_INVALID", "Invalid route configuration"),
    SCHEDULE_INVALID("BUS_SCHEDULE_INVALID", "Invalid schedule dates or times"),
    SCHEDULE_UNAVAILABLE("BUS_SCHEDULE_UNAVAILABLE", "Schedule is marked as unavailable or cancelled"),
    INVALID_STATE_TRANSITION("BUS_INVALID_STATE", "Requested state transition is not allowed"),

    // Seat & Inventory Errors
    SEAT_NOT_FOUND("BUS_SEAT_NOT_FOUND", "Specified seat not found on bus or schedule"),
    SEAT_NOT_AVAILABLE("BUS_SEAT_NOT_AVAILABLE", "One or more requested seats are not available"),
    SEAT_ALREADY_LOCKED("BUS_SEAT_ALREADY_LOCKED", "One or more seats are currently locked by another session"),
    SEAT_LOCK_EXPIRED("BUS_SEAT_LOCK_EXPIRED", "Seat lock has expired"),
    SEAT_LOCK_MISMATCH("BUS_SEAT_LOCK_MISMATCH", "Seat lock token does not match"),
    SEAT_BLOCKED("BUS_SEAT_BLOCKED", "Seat is blocked for administrative or crew use"),

    // Integration Errors
    BOOKING_INTEGRATION_ERROR("BUS_BOOKING_INTEGRATION_ERROR", "Error communicating with central booking integration"),
    EXTERNAL_SERVICE_UNAVAILABLE("BUS_EXTERNAL_UNAVAILABLE", "External service is currently unavailable");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
