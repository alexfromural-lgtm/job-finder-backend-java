package com.jobfinder.domain;

// Jackson serialization annotation to bypass lazy proxy fields
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// JPA annotations to specify entity mappings, tables, columns, indexes, and relations
import jakarta.persistence.*;
// Lombok annotations to reduce boilerplate getters/setters/constructors/builders
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// Hibernate timestamp generation annotations
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// standard Java utility classes for dates and UUID generation
import java.time.Instant;
import java.util.UUID;

/**
 * Mapped from Prisma model Job.
 * Includes the same four performance indexes as the Prisma schema.
 */
// Marks this class as a database-backed entity
@Entity
// Configures the table name as "jobs" and maps the indexes defined in the Prisma schema for fast querying
@Table(
    name = "jobs",
    indexes = {
        // Index on job status (active/inactive) for dashboard and public job lists filtering
        @Index(name = "idx_job_is_active",                  columnList = "is_active"),
        // Composite index for filtering active jobs in specific job categories
        @Index(name = "idx_job_is_active_category",         columnList = "is_active, category"),
        // Composite index to quickly fetch jobs created by a recruiter, sorted by newest first
        @Index(name = "idx_job_recruiter_created",          columnList = "recruiter_id, created_at DESC"),
        // Composite index to retrieve all active jobs ordered by their posting date in descending order
        @Index(name = "idx_job_is_active_created",          columnList = "is_active, created_at DESC")
    }
)
// Lombok generation of standard getters, setters, toString, equals, and hashcode
@Data
// Implements builder design pattern for Job entity construction
@Builder
// Generates the default no-argument constructor required by Hibernate/JPA
@NoArgsConstructor
// Generates the constructor taking all fields as arguments, required by the builder pattern
@AllArgsConstructor
// Directs Jackson not to serialize Hibernate internal helper fields for lazy-loaded fields
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job {

    // Marks this field as the primary key
    @Id
    // Generates a random UUID automatically when saving a new job
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Many-to-one relationship back to the recruiter profile who posted this job
    @ManyToOne(fetch = FetchType.LAZY)
    // Connects via foreign key column "recruiter_id" in the jobs database table
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterProfile recruiter;

    // The title of the job listing, marked as non-nullable
    @Column(nullable = false)
    private String title;

    // Large text field containing the job's main description
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Large text field defining the candidate requirements for this job
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirements;

    // The physical or remote work location of the job, marked as non-nullable
    @Column(nullable = false)
    private String location;

    // Optional salary description or range string
    private String salaryRange;

    // The sector/category of the job listing (e.g. IT, Finance, HR)
    private String category;

    // Boolean flag indicating whether the job post is open/active
    @Column(name = "is_active", nullable = false)
    // Default value to set for Lombok's builder
    @Builder.Default
    private boolean isActive = true;

    // Database timestamp marking when the job was posted; non-updatable
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Database timestamp marking when the job posting details were last modified
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
