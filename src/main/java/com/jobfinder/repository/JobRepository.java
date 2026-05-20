package com.jobfinder.repository;

import com.jobfinder.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByRecruiterIdOrderByCreatedAtDesc(UUID recruiterId);

    /**
     * Paginated, filtered job search.
     * Replaces Prisma's complex where clause with ILIKE for case-insensitive search.
     *
     * All filters are optional — null values are skipped via COALESCE/OR tricks.
     */
    @Query("""
        SELECT j FROM Job j
        WHERE j.isActive = true
          AND (:category IS NULL OR j.category = :category)
          AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:search IS NULL OR
               LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(j.requirements) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY j.createdAt DESC
        """)
    Page<Job> findAllWithFilters(
        @Param("category") String category,
        @Param("location") String location,
        @Param("search")   String search,
        Pageable pageable
    );
}
