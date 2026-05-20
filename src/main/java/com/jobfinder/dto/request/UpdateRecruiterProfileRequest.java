package com.jobfinder.dto.request;

public record UpdateRecruiterProfileRequest(
    String companyName,
    String companyWebsite,
    String description,
    String industry
) {}
