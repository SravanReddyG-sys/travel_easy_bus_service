package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusDomainException {

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE,
                String.format("%s with %s '%s' already exists", resourceName, field, value));
    }

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
