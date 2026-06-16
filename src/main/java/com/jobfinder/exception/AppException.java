package com.jobfinder.exception;

// Lombok annotation to automatically generate getter methods for all private fields
import lombok.Getter;
// Spring HTTP status code enumeration
import org.springframework.http.HttpStatus;

/**
 * Base application exception. Replaces AppError from Node.js backend.
 * Carries an HTTP status so GlobalExceptionHandler can respond correctly.
 */
// Automatically generates getter for the 'status' field
@Getter
public class AppException extends RuntimeException {

    // The HTTP status code associated with this specific exception
    private final HttpStatus status;

    // Constructor that accepts an error message and an HTTP status
    public AppException(String message, HttpStatus status) {
        // Pass the message to the parent RuntimeException class
        super(message);
        // Store the HTTP status code locally
        this.status = status;
    }
}
