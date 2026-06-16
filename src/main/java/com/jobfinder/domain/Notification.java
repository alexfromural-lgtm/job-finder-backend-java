package com.jobfinder.domain;

// Import our custom notification type enum
import com.jobfinder.enums.NotificationType;
// JPA annotations for configuring mappings, tables, columns, and relationships
import jakarta.persistence.*;
// Hibernate JDBC type mapping support
import org.hibernate.annotations.JdbcType;
// Postgres-specific enum support in Hibernate
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
// Lombok annotations to reduce boilerplate constructors, builders, and methods
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
 * Mapped from Prisma model Notification.
 */
// Marks this class as a JPA entity
@Entity
// Specifies the database table name as "notifications"
@Table(name = "notifications")
// Lombok generation of standard getters, setters, toString, equals, and hashCode
@Data
// Implements builder design pattern
@Builder
// Generates default no-arguments constructor
@NoArgsConstructor
// Generates all-arguments constructor
@AllArgsConstructor
public class Notification {

    // Marks this field as the primary key
    @Id
    // Generates a random UUID automatically on save
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Many notifications can be sent to one user (Many-to-One), fetched lazily
    @ManyToOne(fetch = FetchType.LAZY)
    // Links using the foreign key column "user_id" in the notifications table
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Instructs JPA to store the enum value as a string in the database
    @Enumerated(EnumType.STRING)
    // Maps the Enum type to PostgreSQL custom enum column type using Hibernate support
    @JdbcType(PostgreSQLEnumJdbcType.class)
    // Marks the column as non-nullable
    @Column(nullable = false)
    private NotificationType type;

    // Flag indicating whether the notification has been read by the user
    @Column(nullable = false)
    // Default value configuration for builder pattern
    @Builder.Default
    private boolean isRead = false;

    // DB timestamp representing when the notification was created, non-updatable
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
