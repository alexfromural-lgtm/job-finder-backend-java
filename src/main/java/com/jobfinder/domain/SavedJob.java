package com.jobfinder.domain;

// Jackson annotations to configure serialisation and exclude circular dependencies proxy properties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// JPA annotations for defining entity mappings, tables, columns, relations, and constraints
import jakarta.persistence.*;
// Lombok annotations for generating constructors, builders, and standard method implementations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// Hibernate timestamp generation
import org.hibernate.annotations.CreationTimestamp;

// Standard Java classes for time and UUIDs
import java.time.Instant;
import java.util.UUID;

/**
 * Mapped from Prisma model SavedJob.
 * Unique constraint on (job_id, job_seeker_id) matches Prisma @@unique([jobId, jobSeekerId]).
 */
// Marks this class as a database entity
@Entity
// Maps this entity to the "saved_jobs" table and applies a unique constraint on job_id and job_seeker_id
@Table(
    name = "saved_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_saved_job_seeker",
        columnNames = {"job_id", "job_seeker_id"}
    )
)
// Generates boilerplate getters, setters, toString, equals, and hashCode methods
@Data
// Implements the builder design pattern
@Builder
// Generates default no-arguments constructor
@NoArgsConstructor
// Generates constructor taking all fields as parameters
@AllArgsConstructor
// Directs Jackson not to serialize Hibernate internal helper fields for lazy-loaded fields
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SavedJob {

    // Marks this field as the primary key
    @Id
    // Automatically generates a random UUID on entity creation
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Many saved jobs references belong to one Job (Many-to-One), fetched lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links using the foreign key column "job_id" which cannot be null
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Many saved jobs references belong to one JobSeekerProfile (Many-to-One), fetched lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links using the foreign key column "job_seeker_id" which cannot be null
    @JoinColumn(name = "job_seeker_id", nullable = false)
    private JobSeekerProfile jobSeeker;

    // Database timestamp marking when the job seeker saved this job listing; non-updatable
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant savedAt;

    // Helper method to output a direct "jobId" property in JSON responses
    @JsonProperty("jobId")
    public UUID getJobId() {
        // Returns the associated Job's ID if job entity is loaded/present
        return job != null ? job.getId() : null;
    }

    // Helper method to output a direct "jobSeekerId" property in JSON responses
    @JsonProperty("jobSeekerId")
    public UUID getJobSeekerId() {
        // Returns the associated JobSeekerProfile's ID if profile entity is loaded/present
        return jobSeeker != null ? jobSeeker.getId() : null;
    }
}
