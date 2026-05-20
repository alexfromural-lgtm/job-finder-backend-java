package com.jobfinder.exception;

import org.springframework.http.HttpStatus;

/** 409 Conflict */
public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
