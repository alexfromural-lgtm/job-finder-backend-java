package com.jobfinder.dto.request;

// Jakarta validation constraints to check for non-blank text, correct email format, and field size
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Represents the credentials submitted during user login
public record LoginRequest(
    // The login email address, validated to ensure proper format and non-blank
    @NotBlank @Email String email,
    // The login password, validated to be non-blank and at least 6 characters long
    @NotBlank @Size(min = 6) String password
) {}
