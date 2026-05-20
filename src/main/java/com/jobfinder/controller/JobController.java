package com.jobfinder.controller;

import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.request.UpdateJobRequest;
import com.jobfinder.dto.response.JobResponse;
import com.jobfinder.dto.response.PagedJobResponse;
import com.jobfinder.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Replaces src/routes/job.route.ts + src/controllers/job.controller.ts
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // GET /api/jobs/all  (public, paginated + filtered)
    @GetMapping("/all")
    public ResponseEntity<PagedJobResponse> getAllJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(jobService.getAllJobs(category, location, search, page, pageSize));
    }

    // POST /api/jobs  (RECRUITER)
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest req,
                                                  HttpServletRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(userId, req));
    }

    // GET /api/jobs/recruiter  (RECRUITER — must be before /{id})
    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<JobResponse>> getJobsByRecruiter() {
        return ResponseEntity.ok(jobService.getJobsByRecruiter(currentUserId()));
    }

    // GET /api/jobs/{id}  (public)
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // PUT /api/jobs/{id}  (RECRUITER)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<JobResponse> updateJob(@PathVariable UUID id,
                                                  @RequestBody UpdateJobRequest req) {
        return ResponseEntity.ok(jobService.updateJob(id, currentUserId(), req));
    }

    // DELETE /api/jobs/{id}  (RECRUITER)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
