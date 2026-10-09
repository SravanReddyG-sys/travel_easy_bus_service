package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusDomainException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }
}
