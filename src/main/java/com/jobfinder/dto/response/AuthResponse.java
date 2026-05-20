package com.jobfinder.dto.response;

import com.jobfinder.enums.Role;

import java.util.List;
import java.util.UUID;

/**
 * Returned by signup, login, and refresh-token endpoints.
 * The actual tokens are set as HTTP-only cookies; this body
 * only carries the user info (same shape as Node.js backend).
 */
public record AuthResponse(
    UUID userId,
    List<Role> roles
) {}
