package com.jobfinder.service;

// Import our domain entities
import com.jobfinder.domain.Application;
import com.jobfinder.domain.Job;
import com.jobfinder.domain.JobSeekerProfile;
import com.jobfinder.domain.SavedJob;
// Import our payload and response DTO records
import com.jobfinder.dto.request.UpdateJobSeekerProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
// Import custom exceptions mapping to client error shapes
import com.jobfinder.exception.ConflictException;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
// Import persistence repository interfaces
import com.jobfinder.repository.ApplicationRepository;
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.JobSeekerProfileRepository;
import com.jobfinder.repository.SavedJobRepository;
// Lombok annotations for generating constructors and loggers
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// HTTP statuses and exception types for Spring framework web handling
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Standard Java collections and unique identifier utilities
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/jobseeker.service.ts.
 * applyToJob and saveJob are also called directly from DbWriteWorker
 * to preserve the async queue pattern.
 */
// Registers this class as a Spring Service bean
@Service
// Auto-generates constructor injecting final fields
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class JobSeekerService {

    // Inject JobSeekerProfileRepository bean
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    // Inject JobRepository bean
    private final JobRepository              jobRepository;
    // Inject ApplicationRepository bean
    private final ApplicationRepository      applicationRepository;
    // Inject SavedJobRepository bean
    private final SavedJobRepository         savedJobRepository;

    // -------------------------------------------------------------------
    // getJobSeekerProfile
    // -------------------------------------------------------------------

    // Fetches the candidate profile associated with the user ID, or throws ResourceNotFoundException
    public JobSeekerProfile getJobSeekerProfile(UUID userId) {
        return jobSeekerProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Job seeker profile not found"));
    }

    // -------------------------------------------------------------------
    // updateJobSeekerProfile
    // -------------------------------------------------------------------

    // Performs partial updates on the candidate's profile in a transaction context
    @Transactional
    public JobSeekerProfile updateJobSeekerProfile(UUID userId,
                                                    UpdateJobSeekerProfileRequest req) {
        // Retrieve profile details
        JobSeekerProfile profile = getJobSeekerProfile(userId);

        // Apply fields selectively if they are supplied in the request payload
        if (req.bio()        != null) profile.setBio(req.bio());
        if (req.location()   != null) profile.setLocation(req.location());
        if (req.skills()     != null) profile.setSkills(req.skills());
        if (req.education()  != null) profile.setEducation(req.education());
        if (req.experience() != null) profile.setExperience(req.experience());
        if (req.resumeUrl()  != null) profile.setResumeUrl(req.resumeUrl());

        // Persist modified profile properties
        return jobSeekerProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------
    // applyToJob  (called by HTTP controller via queue worker)
    // -------------------------------------------------------------------

    // Submits a candidate job application in a database transaction context
    @Transactional
    public Application applyToJob(UUID userId, UUID jobId, String coverLetter) {
        // Retrieve the candidate's profile associated with the user ID
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        // Retrieve the job listing or throw ResourceNotFoundException
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Throw GONE (410) if the job listing is no longer open/active
        if (!job.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This job is no longer active");
        }

        // Throw ConflictException if candidate already submitted an application to this job
        applicationRepository.findByJobIdAndJobSeekerId(jobId, seeker.getId())
            .ifPresent(a -> { throw new ConflictException("You have already applied to this job"); });

        // Build the Application entity mapping the candidate, job, and cover letter
        Application application = Application.builder()
            .job(job)
            .jobSeeker(seeker)
            .coverLetter(coverLetter)
            .build();

        // Persist application entity to database
        return applicationRepository.save(application);
    }

    // -------------------------------------------------------------------
    // getApplications
    // -------------------------------------------------------------------

    // Retrieves all job applications submitted by the candidate
    public List<ApplicationResponse> getApplications(UUID userId) {
        // Retrieve candidate details
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        // Query application repository and map results to ApplicationResponse DTO records
        return applicationRepository
            .findByJobSeekerIdOrderByCreatedAtDesc(seeker.getId())
            .stream()
            .map(this::toApplicationResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // withdrawApplication
    // -------------------------------------------------------------------

    // Withdraws (removes) a candidate's application in a database transaction context
    @Transactional
    public void withdrawApplication(UUID userId, UUID applicationId) {
        // Retrieve candidate details
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        // Retrieve the application by ID or throw ResourceNotFoundException
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Throw ForbiddenException if candidate does not own the application
        if (!application.getJobSeeker().getId().equals(seeker.getId())) {
            throw new ForbiddenException("You are not authorized to withdraw this application");
        }

        // Delete the application from database
        applicationRepository.delete(application);
    }

    // -------------------------------------------------------------------
    // saveJob  (called by HTTP controller via queue worker)
    // -------------------------------------------------------------------

    // Saves a job to the candidate's bookmarks in a database transaction context
    @Transactional
    public SavedJob saveJob(UUID userId, UUID jobId) {
        // Retrieve candidate profile
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        // Retrieve job listing by ID or throw ResourceNotFoundException
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Throw ConflictException if candidate already bookmarked the job
        savedJobRepository.findByJob_IdAndJobSeeker_Id(jobId, seeker.getId())
            .ifPresent(s -> { throw new ConflictException("Job already saved"); });

        // Build SavedJob mapping reference
        SavedJob savedJob = SavedJob.builder()
            .job(job)
            .jobSeeker(seeker)
            .build();

        // Save bookmarked job to database
        return savedJobRepository.save(savedJob);
    }

    // -------------------------------------------------------------------
    // getSavedJobs
    // -------------------------------------------------------------------

    // Returns a list of all jobs saved by the candidate
    public List<SavedJob> getSavedJobs(UUID userId) {
        // Retrieve candidate details
        JobSeekerProfile seeker = getJobSeekerProfile(userId);
        // Query saved jobs list sorted by saved timestamp in descending order
        return savedJobRepository.findByJobSeeker_IdOrderBySavedAtDesc(seeker.getId());
    }

    // -------------------------------------------------------------------
    // unsaveJob
    // -------------------------------------------------------------------

    // Removes a job bookmark in a database transaction context
    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        // Retrieve candidate details
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        // Retrieve bookmarked job entry or throw ResourceNotFoundException
        SavedJob savedJob = savedJobRepository.findByJob_IdAndJobSeeker_Id(jobId, seeker.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Saved job not found"));

        // Delete bookmarked job from database
        savedJobRepository.delete(savedJob);
    }

    // -------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------

    // Converts an Application entity to its detailed ApplicationResponse DTO record
    public ApplicationResponse toApplicationResponse(Application a) {
        Job job = a.getJob();
        return new ApplicationResponse(
            a.getId(),
            job.getId(),
            a.getJobSeeker().getId(),
            a.getCoverLetter(),
            a.getStatus(),
            a.getCreatedAt(),
            a.getUpdatedAt(),
            new ApplicationResponse.JobInfo(
                job.getId(),
                job.getTitle(),
                job.getLocation(),
                job.getSalaryRange(),
                job.getCategory(),
                job.isActive(),
                new ApplicationResponse.RecruiterInfo(
                    job.getRecruiter().getCompanyName()
                )
            )
        );
    }
}
