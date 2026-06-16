package com.jobfinder.dto.response;

// Jackson annotation to maintain exact casing mappings for property fields in JSON
import com.fasterxml.jackson.annotation.JsonProperty;
// Import our custom Role enum
import com.jobfinder.enums.Role;

// Standard Java classes for time, lists, and unique identifiers
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Represents the response body containing non-sensitive user details
public record UserResponse(
    // Unique identifier of the user
    UUID id,
    // Display name of the user
    String name,
    // Registered email address of the user
    String email,
    // List of roles/authorizations assigned to the user
    List<Role> roles,
    // Custom name mapping to maintain camelCase formatting in JSON responses
    @JsonProperty("isActive") boolean isActive,
    // User creation timestamp
    Instant createdAt,
    // User modification timestamp
    Instant updatedAt
) {}
