package com.jobfinder.dto.response;

import com.jobfinder.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    List<Role> roles,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
