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
     * Uses a native query to avoid Hibernate 6's lower(bytea) type resolution
     * bug that occurs when LOWER() is applied to TEXT columns in JPQL.
     */
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
        nativeQuery = true
    )
    Page<Job> findAllWithFilters(
        @Param("category") String category,
        @Param("location") String location,
        @Param("search")   String search,
        Pageable pageable
    );
}
