package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class InvalidScheduleException extends BusDomainException {

    public InvalidScheduleException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.SCHEDULE_INVALID, message);
    }
}
