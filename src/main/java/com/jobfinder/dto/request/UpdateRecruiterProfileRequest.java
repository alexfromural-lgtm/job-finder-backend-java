package com.jobfinder.dto.request;

// Represents the payload for updating a recruiter's company profile
public record UpdateRecruiterProfileRequest(
    // The updated name of the recruiter's company
    String companyName,
    // The updated website URL of the recruiter's company
    String companyWebsite,
    // The updated description of the recruiter's company
    String description,
    // The updated industry category of the recruiter's company
    String industry
) {}
