package com.jobfinder.repository;

// Import our RecruiterProfile domain entity
import com.jobfinder.domain.RecruiterProfile;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// standard Java classes for Optional values and UUIDs
import java.util.Optional;
import java.util.UUID;

// Marks this interface as a data repository for RecruiterProfile entities
@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {

    // Resolves to "user.id" to find the recruiter profile by the nested User's unique identifier
    Optional<RecruiterProfile> findByUser_Id(UUID userId);
}
