package com.jobfinder.controller;

// Import payload request records
import com.jobfinder.dto.request.ApplyToJobRequest;
import com.jobfinder.dto.request.UpdateJobSeekerProfileRequest;
// Import background queue operation payloads
import com.jobfinder.queue.ApplyToJobPayload;
import com.jobfinder.queue.DbWriteQueueService;
import com.jobfinder.queue.SaveJobPayload;
// Import JobSeekerService containing candidate business operations
import com.jobfinder.service.JobSeekerService;
// Lombok constructor generation annotation
import lombok.RequiredArgsConstructor;
// Spring Framework response and security classes
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Standard Java maps and unique identifiers
import java.util.Map;
import java.util.UUID;

/**
 * Replaces src/routes/jobseeker.route.ts + src/controllers/jobseeker.controller.ts
 *
 * applyToJob and saveJob enqueue the operation and return 202 Accepted
 * with a jobId — the client polls GET /api/queue/job/{jobId} for status.
 */
// Marks this class as a REST Controller handling HTTP requests
@RestController
// Maps the base request path URI for all endpoints in this controller
@RequestMapping("/api/jobseeker")
// Generates standard constructor injecting dependencies automatically
@RequiredArgsConstructor
// Restricts controller access to users holding the JOB_SEEKER role prior to execution
@PreAuthorize("hasRole('JOB_SEEKER')")
public class JobSeekerController {

    // Inject JobSeekerService bean
    private final JobSeekerService    jobSeekerService;
    // Inject DbWriteQueueService bean
    private final DbWriteQueueService queueService;

    // GET /api/jobseeker/profile
    // Returns the profile details of the authenticated candidate
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // Retrieve candidate profile matching authenticated user ID and return nested details
        return ResponseEntity.ok(java.util.Map.of("profile", jobSeekerService.getJobSeekerProfile(currentUserId())));
    }

    // PATCH /api/jobseeker/profile
    // Updates candidate profile details
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateJobSeekerProfileRequest req) {
        // Execute partial updates and return updated profile details
        return ResponseEntity.ok(java.util.Map.of("profile", jobSeekerService.updateJobSeekerProfile(currentUserId(), req)));
    }

    // POST /api/jobseeker/apply/{jobId}  → enqueue, return 202 + jobId
    // Enqueues a job application request to be processed asynchronously
    @PostMapping("/apply/{jobId}")
    public ResponseEntity<Map<String, String>> applyToJob(@PathVariable UUID jobId,
                                                           @RequestBody(required = false)
                                                           ApplyToJobRequest req) {
        String userId = currentUserId().toString();
        // Construct the application message payload mapping the applicant, job, and cover letter
        ApplyToJobPayload payload = ApplyToJobPayload.builder()
            .type("apply-to-job")
            .userId(userId)
            .jobId(jobId.toString())
            .coverLetter(req != null ? req.coverLetter() : null)
            .build();

        // Enqueue application task to the Redis backend queue and receive its unique job ID
        String queueJobId = queueService.enqueue(payload);
        // Return 202 Accepted status carrying tracking details
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("queueJobId", queueJobId, "status", "queued"));
    }

    // GET /api/jobseeker/applications
    // Returns all job applications submitted by the candidate
    @GetMapping("/applications")
    public ResponseEntity<?> getApplications() {
        // Retrieve application list matching candidate ID
        return ResponseEntity.ok(Map.of("applications", jobSeekerService.getApplications(currentUserId())));
    }

    // DELETE /api/jobseeker/applications/{id}
    // Withdraws (removes) a candidate's job application
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> withdrawApplication(@PathVariable UUID id) {
        // Execute verification and delete application details from database
        jobSeekerService.withdrawApplication(currentUserId(), id);
        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    // POST /api/jobseeker/saved/{jobId}  → enqueue, return 202 + jobId
    // Enqueues a job bookmark request to be processed asynchronously
    @PostMapping("/saved/{jobId}")
    public ResponseEntity<Map<String, String>> saveJob(@PathVariable UUID jobId) {
        String userId = currentUserId().toString();
        // Build the bookmark payload mapping the applicant and target job listing
        SaveJobPayload payload = SaveJobPayload.builder()
            .type("save-job")
            .userId(userId)
            .jobId(jobId.toString())
            .build();

        // Enqueue bookmark task to the Redis backend queue and receive its unique job ID
        String queueJobId = queueService.enqueue(payload);
        // Return 202 Accepted status carrying tracking details
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("queueJobId", queueJobId, "status", "queued"));
    }

    // GET /api/jobseeker/saved
    // Returns a list of all jobs bookmarked by the candidate
    @GetMapping("/saved")
    public ResponseEntity<?> getSavedJobs() {
        // Retrieve bookmarks list matching candidate ID
        return ResponseEntity.ok(Map.of("savedJobs", jobSeekerService.getSavedJobs(currentUserId())));
    }

    // DELETE /api/jobseeker/saved/{jobId}
    // Removes a job bookmark from candidate saved list
    @DeleteMapping("/saved/{jobId}")
    public ResponseEntity<Void> unsaveJob(@PathVariable UUID jobId) {
        // Execute deletion checks and delete bookmark link details
        jobSeekerService.unsaveJob(currentUserId(), jobId);
        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    // Resolves authenticated UUID string from current Spring Security context
    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
