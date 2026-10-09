package com.traveleasy.bus.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusDomainException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusDomainException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = ErrorCode.INVALID_REQUEST;
    }

    public BusDomainException(ErrorCode errorCode, String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = errorCode;
    }

    public BusDomainException(HttpStatus status, ErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusDomainException(HttpStatus status, ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
