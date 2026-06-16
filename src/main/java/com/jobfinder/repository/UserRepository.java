package com.jobfinder.repository;

// Import our User domain entity
import com.jobfinder.domain.User;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// standard Java classes for Optional values and UUIDs
import java.util.Optional;
import java.util.UUID;

// Marks this interface as a data repository for User entities
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Custom query method retrieving a user by login email
    Optional<User> findByEmail(String email);

    // Custom query method checking if a user account exists with the specified email
    boolean existsByEmail(String email);
}
