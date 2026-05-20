package com.jobfinder.controller;

import com.jobfinder.dto.request.ApplyToJobRequest;
import com.jobfinder.dto.request.UpdateJobSeekerProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
import com.jobfinder.dto.response.QueueJobStatusResponse;
import com.jobfinder.queue.ApplyToJobPayload;
import com.jobfinder.queue.DbWriteQueueService;
import com.jobfinder.queue.SaveJobPayload;
import com.jobfinder.service.JobSeekerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces src/routes/jobseeker.route.ts + src/controllers/jobseeker.controller.ts
 *
 * applyToJob and saveJob enqueue the operation and return 202 Accepted
 * with a jobId — the client polls GET /api/queue/job/{jobId} for status.
 */
@RestController
@RequestMapping("/api/jobseeker")
@RequiredArgsConstructor
@PreAuthorize("hasRole('JOB_SEEKER')")
public class JobSeekerController {

    private final JobSeekerService    jobSeekerService;
    private final DbWriteQueueService queueService;

    // GET /api/jobseeker/profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(java.util.Map.of("profile", jobSeekerService.getJobSeekerProfile(currentUserId())));
    }

    // PATCH /api/jobseeker/profile
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateJobSeekerProfileRequest req) {
        return ResponseEntity.ok(java.util.Map.of("profile", jobSeekerService.updateJobSeekerProfile(currentUserId(), req)));
    }

    // POST /api/jobseeker/apply/{jobId}  → enqueue, return 202 + jobId
    @PostMapping("/apply/{jobId}")
    public ResponseEntity<Map<String, String>> applyToJob(@PathVariable UUID jobId,
                                                           @RequestBody(required = false)
                                                           ApplyToJobRequest req) {
        String userId = currentUserId().toString();
        ApplyToJobPayload payload = ApplyToJobPayload.builder()
            .type("apply-to-job")
            .userId(userId)
            .jobId(jobId.toString())
            .coverLetter(req != null ? req.coverLetter() : null)
            .build();

        String queueJobId = queueService.enqueue(payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("queueJobId", queueJobId, "status", "queued"));
    }

    // GET /api/jobseeker/applications
    @GetMapping("/applications")
    public ResponseEntity<?> getApplications() {
        return ResponseEntity.ok(Map.of("applications", jobSeekerService.getApplications(currentUserId())));
    }

    // DELETE /api/jobseeker/applications/{id}
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> withdrawApplication(@PathVariable UUID id) {
        jobSeekerService.withdrawApplication(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/jobseeker/saved/{jobId}  → enqueue, return 202 + jobId
    @PostMapping("/saved/{jobId}")
    public ResponseEntity<Map<String, String>> saveJob(@PathVariable UUID jobId) {
        String userId = currentUserId().toString();
        SaveJobPayload payload = SaveJobPayload.builder()
            .type("save-job")
            .userId(userId)
            .jobId(jobId.toString())
            .build();

        String queueJobId = queueService.enqueue(payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("queueJobId", queueJobId, "status", "queued"));
    }

    // GET /api/jobseeker/saved
    @GetMapping("/saved")
    public ResponseEntity<?> getSavedJobs() {
        return ResponseEntity.ok(Map.of("savedJobs", jobSeekerService.getSavedJobs(currentUserId())));
    }

    // DELETE /api/jobseeker/saved/{jobId}
    @DeleteMapping("/saved/{jobId}")
    public ResponseEntity<Void> unsaveJob(@PathVariable UUID jobId) {
        jobSeekerService.unsaveJob(currentUserId(), jobId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
