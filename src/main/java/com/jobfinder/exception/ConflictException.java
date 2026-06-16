package com.jobfinder.exception;

// Spring class representing HTTP response status codes
import org.springframework.http.HttpStatus;

/** 409 Conflict */
// Represents a conflict error in the application (HTTP status code 409)
public class ConflictException extends AppException {
    // Constructor accepting a specific error message
    public ConflictException(String message) {
        // Calls the superclass (AppException) constructor with the message and CONFLICT status
        super(message, HttpStatus.CONFLICT);
    }
}
