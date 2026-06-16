package com.jobfinder.service;

// Import our domain entities
import com.jobfinder.domain.Job;
import com.jobfinder.domain.RecruiterProfile;
// Import our payload and response DTO records
import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.request.UpdateJobRequest;
import com.jobfinder.dto.response.JobResponse;
import com.jobfinder.dto.response.PagedJobResponse;
// Import custom exceptions mapping to client error shapes
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
// Import persistence repository interfaces
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.RecruiterProfileRepository;
// Lombok annotations for generating constructors and loggers
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring framework classes for pagination handling
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
// Spring annotations to define service layer and transaction boundaries
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/job.service.ts.
 */
// Registers this class as a Spring Service bean
@Service
// Auto-generates constructor injecting final fields
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class JobService {

    // Inject JobRepository bean
    private final JobRepository             jobRepository;
    // Inject RecruiterProfileRepository bean
    private final RecruiterProfileRepository recruiterProfileRepository;

    // -------------------------------------------------------------------
    // createJob
    // -------------------------------------------------------------------

    // Creates a new job posting in a database transaction context
    @Transactional
    public JobResponse createJob(UUID userId, CreateJobRequest req) {
        // Retrieve recruiter profile details or throw ResourceNotFoundException
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        // Build Job entity mapping the recruiter profile and request details
        Job job = Job.builder()
            .recruiter(recruiter)
            .title(req.title())
            .description(req.description())
            .requirements(req.requirements())
            .location(req.location())
            .salaryRange(req.salaryRange())
            .category(req.category())
            .build();

        // Save the job posting to the database and map to JobResponse DTO
        return toResponse(jobRepository.save(job));
    }

    // -------------------------------------------------------------------
    // getJobById
    // -------------------------------------------------------------------

    // Returns a job listing by its unique ID
    public JobResponse getJobById(UUID jobId) {
        // Retrieve job or throw ResourceNotFoundException
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        // Map to JobResponse DTO
        return toResponse(job);
    }

    // -------------------------------------------------------------------
    // updateJob
    // -------------------------------------------------------------------

    // Updates a job listing in a database transaction context
    @Transactional
    public JobResponse updateJob(UUID jobId, UUID userId, UpdateJobRequest req) {
        // Retrieve recruiter profile details
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        // Retrieve job listing details
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Throw ForbiddenException if recruiter does not own the job posting
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to update this job");
        }

        // Apply partial updates (only non-null fields from request payload are updated)
        if (req.title()        != null) job.setTitle(req.title());
        if (req.description()  != null) job.setDescription(req.description());
        if (req.requirements() != null) job.setRequirements(req.requirements());
        if (req.location()     != null) job.setLocation(req.location());
        if (req.salaryRange()  != null) job.setSalaryRange(req.salaryRange());
        if (req.category()     != null) job.setCategory(req.category());
        if (req.isActive()     != null) job.setActive(req.isActive());

        // Persist modified job listing and map to JobResponse DTO
        return toResponse(jobRepository.save(job));
    }

    // -------------------------------------------------------------------
    // deleteJob
    // -------------------------------------------------------------------

    // Deletes a job listing in a database transaction context
    @Transactional
    public void deleteJob(UUID jobId, UUID userId) {
        // Retrieve recruiter details
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        // Retrieve job listing details
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Throw ForbiddenException if recruiter does not own the job posting
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to delete this job");
        }

        // Delete job entity from database
        jobRepository.delete(job);
    }

    // -------------------------------------------------------------------
    // getJobsByRecruiter
    // -------------------------------------------------------------------

    // Retrieves all jobs posted by the recruiter
    public List<JobResponse> getJobsByRecruiter(UUID userId) {
        // Retrieve recruiter details
        RecruiterProfile recruiter = recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        // Query job listings sorted by creation timestamp in descending order and map to JobResponse DTO records
        return jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // getAllJobs (paginated + filtered)
    // -------------------------------------------------------------------

    // Retrieves paginated list of job listings filtered by category, location, and search text
    public PagedJobResponse getAllJobs(String category, String location, String search,
                                       int page, int pageSize) {
        // Enforce safe boundary limits on page indices and sizes
        int safePage     = Math.max(1, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));

        // Query database using custom native search query
        Page<Job> result = jobRepository.findAllWithFilters(
            blankToNull(category),
            blankToNull(location),
            blankToNull(search),
            PageRequest.of(safePage - 1, safePageSize) // Spring pages are 0-indexed
        );

        // Convert the content list to JobResponse DTO records
        List<JobResponse> jobs = result.getContent().stream().map(this::toResponse).toList();

        // Construct paginated response DTO matching the frontend meta shape
        return new PagedJobResponse(
            jobs,
            new PagedJobResponse.Meta(
                result.getTotalElements(),
                safePage,
                safePageSize,
                result.getTotalPages()
            )
        );
    }

    // -------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------

    // Converts a Job entity to its JobResponse DTO record representation
    public JobResponse toResponse(Job job) {
        RecruiterProfile r = job.getRecruiter();
        return new JobResponse(
            job.getId(),
            r.getId(),
            r.getCompanyName(),
            r.getIndustry(),
            r.getCompanyWebsite(),
            job.getTitle(),
            job.getDescription(),
            job.getRequirements(),
            job.getLocation(),
            job.getSalaryRange(),
            job.getCategory(),
            job.isActive(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }

    // Normalizes blank string arguments to null parameters for SQL mapping
    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
