package com.jobfinder.dto.response;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
    UUID id,
    UUID recruiterId,
    String companyName,
    String industry,
    String companyWebsite,
    String title,
    String description,
    String requirements,
    String location,
    String salaryRange,
    String category,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
