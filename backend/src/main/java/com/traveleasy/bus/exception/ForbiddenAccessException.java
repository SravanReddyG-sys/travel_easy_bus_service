package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenAccessException extends BusDomainException {

    public ForbiddenAccessException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, message);
    }

    public static ForbiddenAccessException operatorMismatch() {
        return new ForbiddenAccessException("Authenticated operator does not own this resource");
    }
}
