package com.jobfinder.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String requirements,
    @NotBlank String location,
    String salaryRange,
    String category
) {}
