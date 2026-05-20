package com.jobfinder.service;

import com.jobfinder.domain.Application;
import com.jobfinder.domain.Job;
import com.jobfinder.domain.JobSeekerProfile;
import com.jobfinder.domain.SavedJob;
import com.jobfinder.dto.request.UpdateJobSeekerProfileRequest;
import com.jobfinder.dto.response.ApplicationResponse;
import com.jobfinder.exception.ConflictException;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
import com.jobfinder.repository.ApplicationRepository;
import com.jobfinder.repository.JobRepository;
import com.jobfinder.repository.JobSeekerProfileRepository;
import com.jobfinder.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/jobseeker.service.ts.
 * applyToJob and saveJob are also called directly from DbWriteWorker
 * to preserve the async queue pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobSeekerService {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final JobRepository              jobRepository;
    private final ApplicationRepository      applicationRepository;
    private final SavedJobRepository         savedJobRepository;

    // -------------------------------------------------------------------
    // getJobSeekerProfile
    // -------------------------------------------------------------------

    public JobSeekerProfile getJobSeekerProfile(UUID userId) {
        return jobSeekerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Job seeker profile not found"));
    }

    // -------------------------------------------------------------------
    // updateJobSeekerProfile
    // -------------------------------------------------------------------

    @Transactional
    public JobSeekerProfile updateJobSeekerProfile(UUID userId,
                                                    UpdateJobSeekerProfileRequest req) {
        JobSeekerProfile profile = getJobSeekerProfile(userId);

        if (req.bio()        != null) profile.setBio(req.bio());
        if (req.location()   != null) profile.setLocation(req.location());
        if (req.skills()     != null) profile.setSkills(req.skills());
        if (req.education()  != null) profile.setEducation(req.education());
        if (req.experience() != null) profile.setExperience(req.experience());
        if (req.resumeUrl()  != null) profile.setResumeUrl(req.resumeUrl());

        return jobSeekerProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------
    // applyToJob  (called by HTTP controller via queue worker)
    // -------------------------------------------------------------------

    @Transactional
    public Application applyToJob(UUID userId, UUID jobId, String coverLetter) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This job is no longer active");
        }

        applicationRepository.findByJobIdAndJobSeekerId(jobId, seeker.getId())
            .ifPresent(a -> { throw new ConflictException("You have already applied to this job"); });

        Application application = Application.builder()
            .job(job)
            .jobSeeker(seeker)
            .coverLetter(coverLetter)
            .build();

        return applicationRepository.save(application);
    }

    // -------------------------------------------------------------------
    // getApplications
    // -------------------------------------------------------------------

    public List<ApplicationResponse> getApplications(UUID userId) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        return applicationRepository
            .findByJobSeekerIdOrderByCreatedAtDesc(seeker.getId())
            .stream()
            .map(this::toApplicationResponse)
            .toList();
    }

    // -------------------------------------------------------------------
    // withdrawApplication
    // -------------------------------------------------------------------

    @Transactional
    public void withdrawApplication(UUID userId, UUID applicationId) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getJobSeeker().getId().equals(seeker.getId())) {
            throw new ForbiddenException("You are not authorized to withdraw this application");
        }

        applicationRepository.delete(application);
    }

    // -------------------------------------------------------------------
    // saveJob  (called by HTTP controller via queue worker)
    // -------------------------------------------------------------------

    @Transactional
    public SavedJob saveJob(UUID userId, UUID jobId) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        savedJobRepository.findByJobIdAndJobSeekerId(jobId, seeker.getId())
            .ifPresent(s -> { throw new ConflictException("Job already saved"); });

        SavedJob savedJob = SavedJob.builder()
            .job(job)
            .jobSeeker(seeker)
            .build();

        return savedJobRepository.save(savedJob);
    }

    // -------------------------------------------------------------------
    // getSavedJobs
    // -------------------------------------------------------------------

    public List<SavedJob> getSavedJobs(UUID userId) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);
        return savedJobRepository.findByJobSeekerIdOrderBySavedAtDesc(seeker.getId());
    }

    // -------------------------------------------------------------------
    // unsaveJob
    // -------------------------------------------------------------------

    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        JobSeekerProfile seeker = getJobSeekerProfile(userId);

        SavedJob savedJob = savedJobRepository.findByJobIdAndJobSeekerId(jobId, seeker.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Saved job not found"));

        savedJobRepository.delete(savedJob);
    }

    // -------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------

    public ApplicationResponse toApplicationResponse(Application a) {
        Job job = a.getJob();
        return new ApplicationResponse(
            a.getId(),
            job.getId(),
            job.getTitle(),
            job.getLocation(),
            job.getSalaryRange(),
            job.getCategory(),
            job.getRecruiter().getCompanyName(),
            a.getCoverLetter(),
            a.getStatus(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}
