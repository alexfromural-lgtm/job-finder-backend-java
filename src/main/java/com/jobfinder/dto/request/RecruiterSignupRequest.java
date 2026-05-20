package com.jobfinder.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecruiterSignupRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String companyName,
    String companyWebsite,
    String description,
    String industry
) {}
