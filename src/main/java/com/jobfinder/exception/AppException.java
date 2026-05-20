package com.jobfinder.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception. Replaces AppError from Node.js backend.
 * Carries an HTTP status so GlobalExceptionHandler can respond correctly.
 */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
