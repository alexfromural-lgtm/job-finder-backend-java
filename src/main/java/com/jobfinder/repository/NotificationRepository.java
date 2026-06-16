package com.jobfinder.repository;

// Import our Notification domain entity
import com.jobfinder.domain.Notification;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// Standard Java UUID class
import java.util.UUID;

// Marks this interface as a data repository for Notification entities
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
