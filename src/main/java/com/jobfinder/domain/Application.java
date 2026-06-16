package com.jobfinder.domain;

// Jackson annotations to ignore Hibernate proxy lazy loading properties during JSON serialization
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Imports the application status enumeration
import com.jobfinder.enums.ApplicationStatus;
// Jakarta Persistence annotations to define entities, relationships, columns, and IDs
import jakarta.persistence.*;
// Hibernate annotation to configure database-specific column types
import org.hibernate.annotations.JdbcType;
// Postgres enum mapping support in Hibernate
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
// Lombok annotations to auto-generate boilerplate code (getters, setters, constructors, builders)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// Hibernate annotations to automatically handle creation and update timestamps
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// Standard Java classes for time representation and unique identifiers
import java.time.Instant;
import java.util.UUID;

/**
 * Mapped from Prisma model Application.
 */
// Marks this class as a JPA entity mapped to a database table
@Entity
// Maps this entity to the "applications" table in the database
@Table(name = "applications")
// Lombok annotation generating getters, setters, equals, hashCode, and toString methods
@Data
// Lombok annotation enabling the builder design pattern
@Builder
// Generates a no-argument constructor (required by JPA spec)
@NoArgsConstructor
// Generates an all-arguments constructor (needed for Lombok's @Builder)
@AllArgsConstructor
// Directs Jackson serializer to ignore proxy fields introduced by Hibernate's lazy fetching
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Application {

    // Marks this field as the primary key of the entity
    @Id
    // Configures automatic ID generation using the UUID strategy
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Defines a many-to-one relationship with the Job entity, using lazy fetching
    @ManyToOne(fetch = FetchType.LAZY)
    // Configures the foreign key column "job_id" which cannot be null
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Defines a many-to-one relationship with the JobSeekerProfile entity, using lazy fetching
    @ManyToOne(fetch = FetchType.LAZY)
    // Configures the foreign key column "job_seeker_id" which cannot be null
    @JoinColumn(name = "job_seeker_id", nullable = false)
    private JobSeekerProfile jobSeeker;

    // Maps this field to a TEXT database column (allowing long cover letters)
    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    // Tells JPA to store the enum value as a string in the database
    @Enumerated(EnumType.STRING)
    // Custom Hibernate type setting for native PostgreSQL enum compatibility
    @JdbcType(PostgreSQLEnumJdbcType.class)
    // Maps to a non-nullable database column
    @Column(nullable = false)
    // Sets a default value for the builder pattern
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.submitted;

    // Automatically populates this field with the current system time upon entity insertion
    @CreationTimestamp
    // Maps to a non-nullable, non-updatable database column
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Automatically updates this field with the current system time upon entity modification
    @UpdateTimestamp
    // Maps to a non-nullable database column
    @Column(nullable = false)
    private Instant updatedAt;
}
