package com.jobfinder.controller;

// Import our payload request records
import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.request.UpdateJobRequest;
// Import our paginated response DTO
import com.jobfinder.dto.response.PagedJobResponse;
// Import JobService containing business query methods
import com.jobfinder.service.JobService;
// Jakarta Servlet and Spring validation classes
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
// Lombok annotation for constructor generation
import lombok.RequiredArgsConstructor;
// Spring Framework response and security classes
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Standard Java unique identifier utility class
import java.util.UUID;

/**
 * Replaces src/routes/job.route.ts + src/controllers/job.controller.ts
 */
// Marks this class as a REST Controller handling HTTP requests
@RestController
// Maps the base request path URI for all endpoints in this controller
@RequestMapping("/api/jobs")
// Generates standard constructor injecting dependencies automatically
@RequiredArgsConstructor
public class JobController {

    // Inject JobService bean
    private final JobService jobService;

    // GET /api/jobs/all  (public, paginated + filtered)
    // Searches job listings matching filter parameters and page criteria
    @GetMapping("/all")
    public ResponseEntity<PagedJobResponse> getAllJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // Return 200 OK containing paginated job listings DTO
        return ResponseEntity.ok(jobService.getAllJobs(category, location, search, page, pageSize));
    }

    // POST /api/jobs  (RECRUITER)
    // Submits and registers a new job listing
    @PostMapping
    // Restricts access to users holding the RECRUITER role prior to execution
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest req,
                                                   HttpServletRequest request) {
        // Resolve authenticated user ID from security context
        UUID userId = currentUserId();
        // Persist new job listing and return mapped response record in a nested "job" property
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("job", jobService.createJob(userId, req)));
    }

    // GET /api/jobs/recruiter  (RECRUITER — must be before /{id})
    // Returns all jobs posted by the authenticated recruiter
    @GetMapping("/recruiter")
    // Restricts access to users holding the RECRUITER role prior to execution
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getJobsByRecruiter() {
        // Retrieve recruiter user ID and query listings from database
        return ResponseEntity.ok(java.util.Map.of("jobs", jobService.getJobsByRecruiter(currentUserId())));
    }

    // GET /api/jobs/{id}  (public)
    // Returns details of a specific job listing by its unique ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable UUID id) {
        // Retrieve job matching ID path variable and wrap in a nested "job" property
        return ResponseEntity.ok(java.util.Map.of("job", jobService.getJobById(id)));
    }

    // PUT /api/jobs/{id}  (RECRUITER)
    // Updates details of an existing job posting
    @PutMapping("/{id}")
    // Restricts access to users holding the RECRUITER role prior to execution
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> updateJob(@PathVariable UUID id,
                                                   @RequestBody UpdateJobRequest req) {
        // Execute updates, checking recruiter ownership, and return updated job details
        return ResponseEntity.ok(java.util.Map.of("job", jobService.updateJob(id, currentUserId(), req)));
    }

    // DELETE /api/jobs/{id}  (RECRUITER)
    // Deletes an existing job posting
    @DeleteMapping("/{id}")
    // Restricts access to users holding the RECRUITER role prior to execution
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        // Execute deletion checks and delete job entity from database
        jobService.deleteJob(id, currentUserId());
        // Return 240 No Content header details
        return ResponseEntity.noContent().build();
    }

    // Resolves authenticated UUID string from current Spring Security Context
    private UUID currentUserId() {
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }
}
