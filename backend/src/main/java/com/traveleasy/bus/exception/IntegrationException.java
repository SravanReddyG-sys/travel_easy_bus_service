package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends BusDomainException {

    public IntegrationException(String serviceName, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                String.format("[%s Integration] %s", serviceName, message));
    }

    public IntegrationException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, message, cause);
    }
}
