package com.jobfinder.dto.request;

import com.jobfinder.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
    @NotNull ApplicationStatus status
) {}
