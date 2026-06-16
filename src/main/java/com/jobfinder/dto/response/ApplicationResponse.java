package com.jobfinder.dto.response;

// Import our custom application status enum
import com.jobfinder.enums.ApplicationStatus;
// Jackson annotation to customize JSON property names (handling casing mismatches)
import com.fasterxml.jackson.annotation.JsonProperty;

// Standard Java classes for time and unique identifiers
import java.time.Instant;
import java.util.UUID;

// Represents the response body returning full details of a job application
public record ApplicationResponse(
    // Unique identifier of the job application
    UUID id,
    // Unique identifier of the applied job post
    UUID jobId,
    // Unique identifier of the applying job seeker's profile
    UUID jobSeekerId,
    // Candidate's cover letter text
    String coverLetter,
    // Current status of the application
    ApplicationStatus status,
    // Timestamp showing when the application was submitted
    Instant createdAt,
    // Timestamp showing when the application was last updated
    Instant updatedAt,
    // Nested job information record
    JobInfo job
) {
    // Nested record representing brief details about the job
    public record JobInfo(
        // Job unique identifier
        UUID id,
        // The title of the job listing
        String title,
        // The physical or remote work location
        String location,
        // Optional salary range or description
        String salaryRange,
        // Optional sector or category grouping
        String category,
        // Custom name mapping to maintain camelCase formatting in JSON responses
        @JsonProperty("isActive")
        boolean isActive,
        // Nested recruiter details
        RecruiterInfo recruiter
    ) {}

    // Nested record representing basic recruiter/company information
    public record RecruiterInfo(
        // The name of the recruiting company
        String companyName
    ) {}
}
