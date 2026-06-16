package com.jobfinder.repository;

// Import our Report domain entity
import com.jobfinder.domain.Report;
// Spring Data repository interface for basic database operations
import org.springframework.data.jpa.repository.JpaRepository;
// Stereotype indicating this is a repository bean in Spring's container
import org.springframework.stereotype.Repository;

// Standard Java UUID class
import java.util.UUID;

// Marks this interface as a data repository for Report entities
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
}
