package com.jobfinder.exception;

// Spring class representing HTTP response status codes
import org.springframework.http.HttpStatus;

/** 404 Not Found */
// Represents a resource not found error in the application (HTTP status code 404)
public class ResourceNotFoundException extends AppException {
    // Constructor accepting a specific error message
    public ResourceNotFoundException(String message) {
        // Calls the superclass (AppException) constructor with the message and NOT_FOUND status
        super(message, HttpStatus.NOT_FOUND);
    }
}
