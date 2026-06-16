package com.jobfinder.repository;

// Import our SavedJob domain entity
import com.jobfinder.domain.SavedJob;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// standard Java collections and unique identifiers
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Marks this interface as a data repository for SavedJob entities
@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {

    // Custom query method to find a specific saved job reference by Job ID and JobSeekerProfile ID
    Optional<SavedJob> findByJob_IdAndJobSeeker_Id(UUID jobId, UUID jobSeekerId);

    // Custom query method retrieving all saved jobs for a candidate, sorted by saved date in descending order
    List<SavedJob> findByJobSeeker_IdOrderBySavedAtDesc(UUID jobSeekerId);
}
