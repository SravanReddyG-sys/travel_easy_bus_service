package com.traveleasy.bus.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BusDomainException {

    public InvalidStateTransitionException(String entityName, String fromState, String toState) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_STATE_TRANSITION,
                String.format("Cannot transition %s from state '%s' to '%s'", entityName, fromState, toState));
    }

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}
