package com.jobfinder.exception;

// Lombok logging annotation which generates a logger instance named 'log'
import lombok.extern.slf4j.Slf4j;
// Spring classes to structure HTTP responses and define REST status codes
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Spring Security exceptions for authorization and authentication failures
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
// Spring validation classes to inspect binding and validation constraints errors
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
// Spring annotations to handle exceptions globally on REST controllers
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Standard Java collections utility classes
import java.util.HashMap;
import java.util.Map;

/**
 * Replaces errorHandler.middleware.ts from the Node.js backend.
 *
 * All responses follow the same shape used by the Node.js API: { "error": "message" }
 * Validation errors add a "fields" map mirroring Zod's fieldErrors format.
 */
// Registers this class as global exception handling advice for all REST controllers in the application
@RestControllerAdvice
// Generates a logger named 'log'
@Slf4j
public class GlobalExceptionHandler {

    /** AppException subclasses (ResourceNotFound, Conflict, Forbidden, Unauthorized) */
    // Handles custom application-level exceptions (AppException and its subclasses)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(AppException ex) {
        // Logs the exception details at debug level showing status and message
        log.debug("AppException [{}]: {}", ex.getStatus(), ex.getMessage());
        // Returns the response entity with the exception's custom HTTP status and error body
        return ResponseEntity.status(ex.getStatus())
            .body(Map.of("error", ex.getMessage()));
    }

    /** Bean Validation failures (@Valid on request DTOs) — replaces Zod validation errors */
    // Handles validation errors when request bodies fail constraint validation (e.g. @NotBlank, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        // Creates a map to hold field-specific error messages
        Map<String, String> fieldErrors = new HashMap<>();
        // Iterates through each validation field error in the binding results
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Puts the field name and its corresponding default error message in the map
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        // Returns a 400 Bad Request response containing the validation failure error summary and detail map
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Validation failed", "fields", fieldErrors));
    }

    /** Spring Security: access denied (403) from @PreAuthorize */
    // Handles access denied errors when a user lacks sufficient privileges/roles (e.g., from @PreAuthorize checks)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        // Returns a 403 Forbidden status with a generic forbidden error message
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Forbidden"));
    }

    /** Spring Security: authentication failure */
    // Handles errors during user authentication processes (e.g. invalid credentials)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {
        // Returns a 401 Unauthorized status with an unauthorized error message
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Unauthorized"));
    }

    /** Catch-all — 500 */
    // Handles all other unexpected/unhandled exceptions in the application
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Logs the detailed stack trace of the unhandled exception at the error level
        log.error("Unhandled exception", ex);
        // Returns a 500 Internal Server Error status with a generic error message to hide internals
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal server error"));
    }
}
