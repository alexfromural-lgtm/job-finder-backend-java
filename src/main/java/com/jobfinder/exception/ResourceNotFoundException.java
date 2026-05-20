package com.jobfinder.exception;

import org.springframework.http.HttpStatus;

/** 404 Not Found */
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
