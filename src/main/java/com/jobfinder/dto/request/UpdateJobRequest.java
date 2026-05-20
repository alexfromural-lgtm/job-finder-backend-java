package com.jobfinder.dto.request;

public record UpdateJobRequest(
    String title,
    String description,
    String requirements,
    String location,
    String salaryRange,
    String category,
    Boolean isActive
) {}
