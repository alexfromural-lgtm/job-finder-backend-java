package com.jobfinder.controller;

import com.jobfinder.dto.request.UpdateApplicationStatusRequest;
import com.jobfinder.dto.request.UpdateRecruiterProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
import com.jobfinder.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Replaces src/routes/recruiter.route.ts + src/controllers/recruiter.controller.ts
 */
@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final RecruiterService recruiterService;

    // GET /api/recruiter/profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(java.util.Map.of("profile", recruiterService.getRecruiterProfile(currentUserId())));
    }

    // PATCH /api/recruiter/profile
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateRecruiterProfileRequest req) {
        return ResponseEntity.ok(java.util.Map.of("profile", recruiterService.updateRecruiterProfile(currentUserId(), req)));
    }

    // GET /api/recruiter/jobs/{jobId}/applications
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(recruiterService.getApplicationsForJob(currentUserId(), jobId));
    }

    // PATCH /api/recruiter/applications/{id}/status
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest req) {
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(currentUserId(), id, req));
    }

    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
