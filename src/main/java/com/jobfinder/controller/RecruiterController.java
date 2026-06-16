package com.jobfinder.controller;

// Import payload request records
import com.jobfinder.dto.request.UpdateApplicationStatusRequest;
import com.jobfinder.dto.request.UpdateRecruiterProfileRequest;
// Import our response DTO mapping application status
import com.jobfinder.dto.response.ApplicationResponse;
// Import RecruiterService containing recruiter business operations
import com.jobfinder.service.RecruiterService;
// Spring validation and security annotations
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/routes/recruiter.route.ts + src/controllers/recruiter.controller.ts
 */
// Marks this class as a REST Controller handling HTTP requests
@RestController
// Maps the base request path URI for all endpoints in this controller
@RequestMapping("/api/recruiter")
// Generates standard constructor injecting dependencies automatically
@RequiredArgsConstructor
// Restricts controller access to users holding the RECRUITER role prior to execution
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    // Inject RecruiterService bean
    private final RecruiterService recruiterService;

    // GET /api/recruiter/profile
    // Returns the profile details of the authenticated recruiter
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // Retrieve recruiter profile matching authenticated user ID and return nested details
        return ResponseEntity.ok(java.util.Map.of("profile", recruiterService.getRecruiterProfile(currentUserId())));
    }

    // PATCH /api/recruiter/profile
    // Updates recruiter company profile details
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateRecruiterProfileRequest req) {
        // Execute updates and return updated profile details
        return ResponseEntity.ok(java.util.Map.of("profile", recruiterService.updateRecruiterProfile(currentUserId(), req)));
    }

    // GET /api/recruiter/jobs/{jobId}/applications
    // Returns a list of all applications submitted to a specific job listing
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
            @PathVariable UUID jobId) {
        // Retrieve applications list, validating recruiter ownership of the target job posting
        return ResponseEntity.ok(recruiterService.getApplicationsForJob(currentUserId(), jobId));
    }

    // PATCH /api/recruiter/applications/{id}/status
    // Updates the processing status of a job application (e.g. accepted, rejected)
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest req) {
        // Execute status updates, validating recruiter ownership, and return updated application details
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(currentUserId(), id, req));
    }

    // Resolves authenticated UUID string from current Spring Security context
    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
