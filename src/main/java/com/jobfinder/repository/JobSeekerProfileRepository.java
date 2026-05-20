package com.jobfinder.repository;

import com.jobfinder.domain.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, UUID> {

    Optional<JobSeekerProfile> findByUserId(UUID userId);
}
