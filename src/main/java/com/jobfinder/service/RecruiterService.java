package com.jobfinder.service;

// Import our domain entities
import com.jobfinder.domain.Application;
import com.jobfinder.domain.Job;
import com.jobfinder.domain.RecruiterProfile;
// Import our payload and response DTO records
import com.jobfinder.dto.request.UpdateApplicationStatusRequest;
import com.jobfinder.dto.request.UpdateRecruiterProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
// Import custom exceptions mapping to client error shapes
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
// Import persistence repository interfaces
import com.jobfinder.repository.ApplicationRepository;
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.RecruiterProfileRepository;
// Lombok annotations for generating constructors and loggers
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring annotations to register service layer and database transactions
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/recruiter.service.ts.
 */
// Registers this class as a Spring Service bean
@Service
// Auto-generates constructor injecting final fields
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class RecruiterService {

    // Inject RecruiterProfileRepository bean
    private final RecruiterProfileRepository recruiterProfileRepository;
    // Inject JobRepository bean
    private final JobRepository              jobRepository;
    // Inject ApplicationRepository bean
    private final ApplicationRepository      applicationRepository;
    // Inject JobSeekerService bean to reuse its mapper utility
    private final JobSeekerService           jobSeekerService;

    // -------------------------------------------------------------------
    // getRecruiterProfile
    // -------------------------------------------------------------------

    // Retrieves recruiter profile details using the User's unique ID, or throws ResourceNotFoundException
    public RecruiterProfile getRecruiterProfile(UUID userId) {
        return recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
    }

    // -------------------------------------------------------------------
    // updateRecruiterProfile
    // -------------------------------------------------------------------

    // Performs partial updates on the recruiter's profile details in a database transaction context
    @Transactional
    public RecruiterProfile updateRecruiterProfile(UUID userId,
                                                    UpdateRecruiterProfileRequest req) {
        // Retrieve profile details
        RecruiterProfile profile = getRecruiterProfile(userId);

        // Apply fields selectively if they are supplied in the request payload
        if (req.companyName()    != null) profile.setCompanyName(req.companyName());
        if (req.companyWebsite() != null) profile.setCompanyWebsite(req.companyWebsite());
        if (req.description()    != null) profile.setDescription(req.description());
        if (req.industry()       != null) profile.setIndustry(req.industry());

        // Save updated profile
        return recruiterProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------
    // getApplicationsForJob
    // -------------------------------------------------------------------

    // Returns a list of all applications submitted to a specific job posting
    public List<ApplicationResponse> getApplicationsForJob(UUID userId, UUID jobId) {
        // Retrieve recruiter profile details
        RecruiterProfile recruiter = getRecruiterProfile(userId);

        // Retrieve job listing details
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Throw ForbiddenException if recruiter does not own the job posting
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to view applications for this job");
        }

        // Query applications list sorted by creation timestamp in descending order and map to ApplicationResponse DTO records
        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId)
            .stream()
            .map(jobSeekerService::toApplicationResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // updateApplicationStatus
    // -------------------------------------------------------------------

    // Updates the processing status of a job application in a database transaction context
    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID userId, UUID applicationId,
                                                        UpdateApplicationStatusRequest req) {
        // Retrieve recruiter details
        RecruiterProfile recruiter = getRecruiterProfile(userId);

        // Retrieve job application details
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Throw ForbiddenException if recruiter does not own the job posting associated with this application
        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to update this application");
        }

        // Apply new status from request payload
        application.setStatus(req.status());
        // Save modified application state
        Application updated = applicationRepository.save(application);
        // Map to ApplicationResponse DTO record
        return jobSeekerService.toApplicationResponse(updated);
    }
}
