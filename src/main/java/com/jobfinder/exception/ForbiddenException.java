package com.jobfinder.exception;

// Spring class representing HTTP response status codes
import org.springframework.http.HttpStatus;

/** 403 Forbidden */
// Represents a forbidden access error in the application (HTTP status code 403)
public class ForbiddenException extends AppException {
    // Constructor accepting a specific error message
    public ForbiddenException(String message) {
        // Calls the superclass (AppException) constructor with the message and FORBIDDEN status
        super(message, HttpStatus.FORBIDDEN);
    }
}
