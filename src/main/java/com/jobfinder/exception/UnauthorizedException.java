package com.jobfinder.exception;

import org.springframework.http.HttpStatus;

/** 401 Unauthorized */
public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
