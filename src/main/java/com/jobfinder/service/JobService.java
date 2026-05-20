package com.jobfinder.service;

import com.jobfinder.domain.Job;
import com.jobfinder.domain.RecruiterProfile;
import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.request.UpdateJobRequest;
import com.jobfinder.dto.response.JobResponse;
import com.jobfinder.dto.response.PagedJobResponse;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.RecruiterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/job.service.ts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository             jobRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    // -------------------------------------------------------------------
    // createJob
    // -------------------------------------------------------------------

    @Transactional
    public JobResponse createJob(UUID userId, CreateJobRequest req) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        Job job = Job.builder()
            .recruiter(recruiter)
            .title(req.title())
            .description(req.description())
            .requirements(req.requirements())
            .location(req.location())
            .salaryRange(req.salaryRange())
            .category(req.category())
            .build();

        return toResponse(jobRepository.save(job));
    }

    // -------------------------------------------------------------------
    // getJobById
    // -------------------------------------------------------------------

    public JobResponse getJobById(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return toResponse(job);
    }

    // -------------------------------------------------------------------
    // updateJob
    // -------------------------------------------------------------------

    @Transactional
    public JobResponse updateJob(UUID jobId, UUID userId, UpdateJobRequest req) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to update this job");
        }

        // Apply partial updates (only non-null fields)
        if (req.title()        != null) job.setTitle(req.title());
        if (req.description()  != null) job.setDescription(req.description());
        if (req.requirements() != null) job.setRequirements(req.requirements());
        if (req.location()     != null) job.setLocation(req.location());
        if (req.salaryRange()  != null) job.setSalaryRange(req.salaryRange());
        if (req.category()     != null) job.setCategory(req.category());
        if (req.isActive()     != null) job.setActive(req.isActive());

        return toResponse(jobRepository.save(job));
    }

    // -------------------------------------------------------------------
    // deleteJob
    // -------------------------------------------------------------------

    @Transactional
    public void deleteJob(UUID jobId, UUID userId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to delete this job");
        }

        jobRepository.delete(job);
    }

    // -------------------------------------------------------------------
    // getJobsByRecruiter
    // -------------------------------------------------------------------

    public List<JobResponse> getJobsByRecruiter(UUID userId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        return jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // getAllJobs (paginated + filtered)
    // -------------------------------------------------------------------

    public PagedJobResponse getAllJobs(String category, String location, String search,
                                       int page, int pageSize) {
        int safePage     = Math.max(1, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));

        Page<Job> result = jobRepository.findAllWithFilters(
            blankToNull(category),
            blankToNull(location),
            blankToNull(search),
            PageRequest.of(safePage - 1, safePageSize) // Spring pages are 0-indexed
        );

        List<JobResponse> jobs = result.getContent().stream().map(this::toResponse).toList();

        return new PagedJobResponse(
            jobs,
            result.getTotalElements(),
            safePage,
            safePageSize,
            result.getTotalPages()
        );
    }

    // -------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------

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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
