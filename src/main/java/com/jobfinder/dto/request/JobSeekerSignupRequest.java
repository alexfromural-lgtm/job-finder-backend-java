package com.jobfinder.dto.request;

// Jakarta validation constraints to check for non-blank text, correct email format, and field size
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Represents the registration details submitted by a new job seeker candidate
public record JobSeekerSignupRequest(
    // The candidate's name, validated to ensure it is not blank
    @NotBlank String name,
    // The candidate's login email, validated to ensure correct email syntax and non-blank
    @NotBlank @Email String email,
    // The password chosen by candidate, validated to be non-blank and at least 6 characters long
    @NotBlank @Size(min = 6) String password
) {}
