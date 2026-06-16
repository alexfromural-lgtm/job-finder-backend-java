package com.jobfinder.dto.request;

// Represents the payload for updating an existing job listing (all fields are optional)
public record UpdateJobRequest(
    // The updated title of the job listing
    String title,
    // The updated description text of the job listing
    String description,
    // The updated candidate qualifications/requirements text
    String requirements,
    // The updated job location
    String location,
    // The updated salary description or range
    String salaryRange,
    // The updated category for the job
    String category,
    // Flag to enable/disable (active/inactive) the job post
    Boolean isActive
) {}
