package com.jobfinder.dto.response;

// Standard Java classes for time and unique identifiers
import java.time.Instant;
import java.util.UUID;

// Represents the response body containing the detailed view of a job listing
public record JobResponse(
    // Unique identifier of the job listing
    UUID id,
    // Unique identifier of the recruiter who posted the job
    UUID recruiterId,
    // The name of the recruiting company
    String companyName,
    // The industry category of the recruiting company
    String industry,
    // The official website URL of the company
    String companyWebsite,
    // The title of the job listing
    String title,
    // Detailed description of the job posting
    String description,
    // candidate qualifications/requirements text
    String requirements,
    // The physical or remote work location
    String location,
    // Optional salary description or range
    String salaryRange,
    // Optional sector/category classification
    String category,
    // Flag indicating whether the job listing is open/active
    boolean isActive,
    // Timestamp showing when the job was posted
    Instant createdAt,
    // Timestamp showing when the job listing was last updated
    Instant updatedAt
) {}
