package com.jobfinder.dto.response;

import com.jobfinder.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID jobId,
    String jobTitle,
    String jobLocation,
    String salaryRange,
    String category,
    String companyName,
    String coverLetter,
    ApplicationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
