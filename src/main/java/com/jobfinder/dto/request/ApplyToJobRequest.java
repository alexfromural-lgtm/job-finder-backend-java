package com.jobfinder.dto.request;

// Represents the payload sent when a job seeker applies to a job posting
public record ApplyToJobRequest(
    // The optional or required cover letter text submitted with the application
    String coverLetter
) {}
