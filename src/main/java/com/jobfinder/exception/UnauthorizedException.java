package com.jobfinder.exception;

// Spring class representing HTTP response status codes
import org.springframework.http.HttpStatus;

/** 401 Unauthorized */
// Represents an unauthorized access error in the application (HTTP status code 401)
public class UnauthorizedException extends AppException {
    // Constructor accepting a specific error message
    public UnauthorizedException(String message) {
        // Calls the superclass (AppException) constructor with the message and UNAUTHORIZED status
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
