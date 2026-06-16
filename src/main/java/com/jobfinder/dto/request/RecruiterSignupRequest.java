package com.jobfinder.dto.request;

// Jakarta validation constraints to check for non-blank text, correct email format, and field size
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Represents the registration details submitted by a new recruiter user
public record RecruiterSignupRequest(
    // The recruiter's full name, validated to ensure it is not blank
    @NotBlank String name,
    // The recruiter's email, validated to be non-blank and check correct email format
    @NotBlank @Email String email,
    // The recruiter's password, validated to be non-blank and at least 6 characters long
    @NotBlank @Size(min = 6) String password,
    // The name of the company, validated to ensure it is not blank
    @NotBlank String companyName,
    // Optional website link for the recruiter's company
    String companyWebsite,
    // Optional description of the recruiter's company
    String description,
    // Optional industry category of the recruiter's company
    String industry
) {}
