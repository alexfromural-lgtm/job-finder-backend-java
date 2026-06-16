package com.jobfinder.repository;

// Import our Application domain entity class
import com.jobfinder.domain.Application;
// Spring Data JpaRepository providing basic CRUD operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a data access component in the Spring container
import org.springframework.stereotype.Repository;

// standard Java collections and unique identifiers
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Marks this interface as a data repository bean for Application entities
@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // Custom query method finding an application by job ID and job seeker profile ID
    Optional<Application> findByJobIdAndJobSeekerId(UUID jobId, UUID jobSeekerId);

    // Custom query method retrieving all applications of a specific candidate, ordered by newest first
    List<Application> findByJobSeekerIdOrderByCreatedAtDesc(UUID jobSeekerId);

    // Custom query method retrieving all applications for a specific job posting, ordered by newest first
    List<Application> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}
