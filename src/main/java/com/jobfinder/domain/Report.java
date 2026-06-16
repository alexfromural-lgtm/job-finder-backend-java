package com.jobfinder.domain;

// Import our custom report status enum
import com.jobfinder.enums.ReportStatus;
// JPA annotations for mapping database tables, relationships, and columns
import jakarta.persistence.*;
// Hibernate JDBC type mapping support
import org.hibernate.annotations.JdbcType;
// Postgres-specific enum support in Hibernate
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
// Lombok annotations to reduce constructor and builder boilerplate
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
 * Mapped from Prisma model Report.
 * Two separate @ManyToOne to User for reporter ("ReportsMade") and reportedUser ("ReportsReceived").
 */
// Marks this class as a database-backed JPA entity
@Entity
// Maps this entity to the "reports" database table
@Table(name = "reports")
// Lombok generation of standard getters, setters, toString, equals, and hashCode
@Data
// Implements builder design pattern
@Builder
// Generates default no-argument constructor
@NoArgsConstructor
// Generates constructor taking all fields
@AllArgsConstructor
public class Report {

    // Marks this field as the primary key
    @Id
    // Generates a random UUID automatically when storing a new report
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Many reports can be filed by one user, loaded lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links to foreign key column "reporter_id" in the database
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Many reports can target the same user, loaded lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links to foreign key column "reported_user_id" (optional field)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    // Many reports can target the same job posting, loaded lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links to foreign key column "reported_job_id" (optional field)
    @JoinColumn(name = "reported_job_id")
    private Job reportedJob;

    // Detailed explanation of the abuse or reason for reporting, non-nullable
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    // Tells JPA to store the enum value as a string
    @Enumerated(EnumType.STRING)
    // Custom Hibernate type setting for native PostgreSQL enum compatibility
    @JdbcType(PostgreSQLEnumJdbcType.class)
    // Maps to a non-nullable database column
    @Column(nullable = false)
    // Sets a default value for Lombok's builder
    @Builder.Default
    private ReportStatus status = ReportStatus.open;

    // Database timestamp marking when the report was submitted; non-updatable
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
