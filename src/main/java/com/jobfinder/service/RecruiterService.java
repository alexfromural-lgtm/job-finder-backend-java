package com.jobfinder.service;

import com.jobfinder.domain.Application;
import com.jobfinder.domain.Job;
import com.jobfinder.domain.RecruiterProfile;
import com.jobfinder.dto.request.UpdateApplicationStatusRequest;
import com.jobfinder.dto.request.UpdateRecruiterProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
import com.jobfinder.repository.ApplicationRepository;
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.RecruiterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/recruiter.service.ts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruiterService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobRepository              jobRepository;
    private final ApplicationRepository      applicationRepository;
    private final JobSeekerService           jobSeekerService;

    // -------------------------------------------------------------------
    // getRecruiterProfile
    // -------------------------------------------------------------------

    public RecruiterProfile getRecruiterProfile(UUID userId) {
        return recruiterProfileRepository.findByUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
    }

    // -------------------------------------------------------------------
    // updateRecruiterProfile
    // -------------------------------------------------------------------

    @Transactional
    public RecruiterProfile updateRecruiterProfile(UUID userId,
                                                    UpdateRecruiterProfileRequest req) {
        RecruiterProfile profile = getRecruiterProfile(userId);

        if (req.companyName()    != null) profile.setCompanyName(req.companyName());
        if (req.companyWebsite() != null) profile.setCompanyWebsite(req.companyWebsite());
        if (req.description()    != null) profile.setDescription(req.description());
        if (req.industry()       != null) profile.setIndustry(req.industry());

        return recruiterProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------
    // getApplicationsForJob
    // -------------------------------------------------------------------

    public List<ApplicationResponse> getApplicationsForJob(UUID userId, UUID jobId) {
        RecruiterProfile recruiter = getRecruiterProfile(userId);

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to view applications for this job");
        }

        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId)
            .stream()
            .map(jobSeekerService::toApplicationResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // updateApplicationStatus
    // -------------------------------------------------------------------

    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID userId, UUID applicationId,
                                                        UpdateApplicationStatusRequest req) {
        RecruiterProfile recruiter = getRecruiterProfile(userId);

        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new ForbiddenException("You are not authorized to update this application");
        }

        application.setStatus(req.status());
        Application updated = applicationRepository.save(application);
        return jobSeekerService.toApplicationResponse(updated);
    }
}
