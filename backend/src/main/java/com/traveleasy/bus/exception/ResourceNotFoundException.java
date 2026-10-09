package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusDomainException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
