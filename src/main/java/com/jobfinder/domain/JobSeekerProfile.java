package com.jobfinder.domain;

// Jackson annotations to configure property names and ignore infinite recursion fields during serialization
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// JPA annotations for mapping entities, tables, relationships, and collection tables
import jakarta.persistence.*;
// Lombok annotations for getters/setters, builders, and constructor boilerplate
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Standard Java collections and UUID types
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapped from Prisma model JobSeekerProfile.
 * skills → @ElementCollection (String list in a join table).
 */
// Marks this class as a database entity
@Entity
// Maps this entity to the "job_seeker_profiles" table
@Table(name = "job_seeker_profiles")
// Generates boilerplate getters, setters, toString, equals, and hashCode
@Data
// Enables the builder design pattern
@Builder
// Generates a no-argument constructor
@NoArgsConstructor
// Generates an all-arguments constructor
@AllArgsConstructor
// Configures Jackson to ignore Hibernate proxy utilities during JSON output
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JobSeekerProfile {

    // Marks this field as the primary key
    @Id
    // Generates a random UUID automatically for new profiles
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Defines a one-to-one mapping with the User entity, loading it lazily
    @OneToOne(fetch = FetchType.LAZY)
    // Connects via unique foreign key column "user_id" which cannot be null
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Excludes back-references and credentials on the User object during serialization to prevent loops
    @JsonIgnoreProperties({"jobSeeker", "recruiter", "password", "createdAt", "updatedAt", "isActive", "hibernateLazyInitializer", "handler"})
    private User user;

    // Bio text describing the job seeker
    @Column(columnDefinition = "TEXT")
    private String bio;

    // Current location of the job seeker
    private String location;

    // Defines a collection of basic values (Strings) stored in a separate join table
    @ElementCollection(fetch = FetchType.EAGER)
    // Specifies the table name "job_seeker_skills" and its foreign key column "profile_id"
    @CollectionTable(name = "job_seeker_skills", joinColumns = @JoinColumn(name = "profile_id"))
    // Maps the column name inside the collection table to "skill"
    @Column(name = "skill")
    // Sets a default empty ArrayList for the builder pattern
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    // Educational history of the job seeker
    private String education;

    // Detailed professional experience text
    @Column(columnDefinition = "TEXT")
    private String experience;

    // URL to the candidate's uploaded resume file
    private String resumeUrl;

    // Helper method to output a direct "userId" field in JSON responses
    @JsonProperty("userId")
    public UUID getUserId() {
        // Returns the User's ID if user relationship is loaded/present
        return user != null ? user.getId() : null;
    }
}
