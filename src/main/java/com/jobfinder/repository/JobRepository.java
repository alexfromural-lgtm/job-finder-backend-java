package com.jobfinder.repository;

// Import our Job domain entity class
import com.jobfinder.domain.Job;
// Spring Data classes for paginated results and query pageability inputs
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Spring Data annotations for custom queries and parameter bindings
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// Stereotype annotation defining Spring repository beans
import org.springframework.stereotype.Repository;

// standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

// Marks this interface as a data access repository for Job entities
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    // Custom query method retrieving all jobs posted by a recruiter, sorted by newest first
    List<Job> findByRecruiterIdOrderByCreatedAtDesc(UUID recruiterId);

    /**
     * Paginated, filtered job search.
     * Uses a native query to avoid Hibernate 6's lower(bytea) type resolution
     * bug that occurs when LOWER() is applied to TEXT columns in JPQL.
     */
    // Defines custom SQL native query with filters for active flag, category match, location match, and general search text
    @Query(
        value = """
            SELECT * FROM jobs j
            WHERE j.is_active = true
              AND (:category IS NULL OR j.category = :category)
              AND (:location IS NULL OR LOWER(j.location::text) ILIKE LOWER('%' || :location || '%'))
              AND (:search IS NULL OR (
                   LOWER(j.title::text)        ILIKE LOWER('%' || :search || '%')
                 OR LOWER(j.description::text)  ILIKE LOWER('%' || :search || '%')
                 OR LOWER(j.requirements::text) ILIKE LOWER('%' || :search || '%')
               ))
            ORDER BY j.created_at DESC
            """,
        // Parallel count query to compute total pages/counts for the pageable request
        countQuery = """
            SELECT COUNT(*) FROM jobs j
            WHERE j.is_active = true
              AND (:category IS NULL OR j.category = :category)
              AND (:location IS NULL OR LOWER(j.location::text) ILIKE LOWER('%' || :location || '%'))
              AND (:search IS NULL OR (
                   LOWER(j.title::text)        ILIKE LOWER('%' || :search || '%')
                 OR LOWER(j.description::text)  ILIKE LOWER('%' || :search || '%')
                 OR LOWER(j.requirements::text) ILIKE LOWER('%' || :search || '%')
               ))
            """,
        // Tells Spring Data to run the exact SQL string on the database instead of translating JPQL
        nativeQuery = true
    )
    Page<Job> findAllWithFilters(
        // Binds the search category parameter
        @Param("category") String category,
        // Binds the location parameter
        @Param("location") String location,
        // Binds the search term parameter
        @Param("search")   String search,
        // Contains page index, page size, and sorting parameters
        Pageable pageable
    );
}
