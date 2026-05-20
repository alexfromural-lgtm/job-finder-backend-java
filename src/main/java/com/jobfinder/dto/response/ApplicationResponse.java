package com.jobfinder.dto.response;

import com.jobfinder.enums.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID jobId,
    UUID jobSeekerId,
    String coverLetter,
    ApplicationStatus status,
    Instant createdAt,
    Instant updatedAt,
    JobInfo job
) {
    public record JobInfo(
        UUID id,
        String title,
        String location,
        String salaryRange,
        String category,
        @JsonProperty("isActive")
        boolean isActive,
        RecruiterInfo recruiter
    ) {}

    public record RecruiterInfo(
        String companyName
    ) {}
}
