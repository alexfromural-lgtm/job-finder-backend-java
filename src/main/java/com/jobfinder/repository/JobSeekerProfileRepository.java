package com.jobfinder.repository;

// Import our JobSeekerProfile domain entity
import com.jobfinder.domain.JobSeekerProfile;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// standard Java classes for Optional values and UUIDs
import java.util.Optional;
import java.util.UUID;

// Marks this interface as a data repository for JobSeekerProfile entities
@Repository
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, UUID> {

    // Resolves to "user.id" to find the job seeker profile by the nested User's unique identifier
    Optional<JobSeekerProfile> findByUser_Id(UUID userId);
}
