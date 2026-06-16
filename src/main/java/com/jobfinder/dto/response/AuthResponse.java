package com.jobfinder.dto.response;

// Import our custom Role enum
import com.jobfinder.enums.Role;

// Standard Java collections and UUID types
import java.util.List;
import java.util.UUID;

/**
 * Returned by signup, login, and refresh-token endpoints.
 * The actual tokens are set as HTTP-only cookies; this body
 * only carries the user info (same shape as Node.js backend).
 */
// Represents the payload returned on successful user signup, login, or token refresh
public record AuthResponse(
    // The authenticated user's unique identifier
    UUID userId,
    // The list of system roles assigned to the user
    List<Role> roles
) {}
