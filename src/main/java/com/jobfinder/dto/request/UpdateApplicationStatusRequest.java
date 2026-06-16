package com.jobfinder.dto.request;

// Import our custom application status enum
import com.jobfinder.enums.ApplicationStatus;
// Jakarta validation constraint to ensure a field is not null
import jakarta.validation.constraints.NotNull;

// Represents the payload for updating the status of a job application
public record UpdateApplicationStatusRequest(
    // The new status of the job application, validated to ensure it is not null
    @NotNull ApplicationStatus status
) {}
