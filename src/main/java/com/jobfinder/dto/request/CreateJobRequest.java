package com.jobfinder.dto.request;

// Jakarta validation import to ensure fields are not null and contain non-whitespace text
import jakarta.validation.constraints.NotBlank;

// Represents the payload for posting a new job listing
public record CreateJobRequest(
    // The title of the job, validated to ensure it is not blank
    @NotBlank String title,
    // The detailed description of the job, validated to ensure it is not blank
    @NotBlank String description,
    // The requirements and qualifications list, validated to ensure it is not blank
    @NotBlank String requirements,
    // The location of the job (e.g. Remote, City), validated to ensure it is not blank
    @NotBlank String location,
    // Optional salary description or range string
    String salaryRange,
    // Optional category grouping for this job posting
    String category
) {}
