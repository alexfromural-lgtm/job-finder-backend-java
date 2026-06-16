package com.jobfinder.domain;

// Jackson annotations to configure property properties and ignore circular serialization
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// JPA annotations for database entity, table mapping, columns, and relationships
import jakarta.persistence.*;
// Lombok annotations for getters/setters, builders, and constructors boilerplate
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Standard Java class for UUIDs
import java.util.UUID;

/**
 * Mapped from Prisma model RecruiterProfile.
 */
// Marks this class as a database entity
@Entity
// Maps this entity to the "recruiter_profiles" table
@Table(name = "recruiter_profiles")
// Generates boilerplate getters, setters, toString, equals, and hashCode
@Data
// Implements the builder design pattern
@Builder
// Generates the default no-argument constructor
@NoArgsConstructor
// Generates the constructor taking all fields
@AllArgsConstructor
// Directs Jackson not to serialize Hibernate proxy helpers for lazy-loaded fields
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RecruiterProfile {

    // Marks this field as the primary key
    @Id
    // Automatically generates a random UUID when storing a new profile
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Defines a one-to-one relationship with the User entity, fetched lazily
    @OneToOne(fetch = FetchType.LAZY)
    // Connects via unique foreign key column "user_id" which cannot be null
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Excludes credentials and relationships on the User object to prevent infinite recursion
    @JsonIgnoreProperties({"jobSeeker", "recruiter", "password", "createdAt", "updatedAt", "isActive", "hibernateLazyInitializer", "handler"})
    private User user;

    // The name of the company the recruiter represents, non-nullable
    @Column(nullable = false)
    private String companyName;

    // Optional website link for the company
    private String companyWebsite;

    // Detailed description of the company
    @Column(columnDefinition = "TEXT")
    private String description;

    // The sector or industry of the company (e.g. IT, Healthcare)
    private String industry;

    // Helper method to output a direct "userId" field in JSON responses
    @JsonProperty("userId")
    public UUID getUserId() {
        // Returns the associated User's ID if user entity is loaded/present
        return user != null ? user.getId() : null;
    }
}
