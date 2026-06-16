package com.jobfinder.domain;

// Jackson serialization annotation to bypass lazy proxy fields
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Import our custom Role enum
import com.jobfinder.enums.Role;
// JPA annotations for mapping database tables, relationships, and columns
import jakarta.persistence.*;
// Lombok annotations to reduce boilerplate getters/setters/builders/constructors
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// Hibernate timestamp generation annotations
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// Standard Java classes for time, lists, and UUIDs
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapped from Prisma model User.
 * roles → @ElementCollection stored in a separate join table.
 */
// Marks this class as a JPA entity
@Entity
// Maps this entity to the "users" table in the database
@Table(name = "users")
// Generates boilerplate getters, setters, toString, equals, and hashCode
@Data
// Implements builder design pattern
@Builder
// Generates default no-arguments constructor
@NoArgsConstructor
// Generates constructor taking all fields
@AllArgsConstructor
// Directs Jackson not to serialize Hibernate internal helper fields for lazy-loaded fields
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    // Marks this field as the primary key
    @Id
    // Generates a random UUID automatically when saving a new user
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The display name of the user, marked as non-nullable
    @Column(nullable = false)
    private String name;

    // The login email address, marked as unique and non-nullable
    @Column(nullable = false, unique = true)
    private String email;

    // The hashed password of the user, marked as non-nullable
    @Column(nullable = false)
    private String password;

    // Defines a collection of basic values (Role enums) stored in a separate join table, loaded eagerly
    @ElementCollection(fetch = FetchType.EAGER)
    // Specifies the table name "user_roles" and its foreign key column "user_id"
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    // Maps the column name inside the collection table to "role"
    @Column(name = "role")
    // Tells JPA to store the enum value as a string
    @Enumerated(EnumType.STRING)
    // Sets a default empty ArrayList for the builder pattern
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    // Boolean flag indicating whether the user account is active/enabled
    @Column(nullable = false)
    // Default value for builder pattern
    @Builder.Default
    private boolean isActive = true;

    // Database timestamp marking when the user registered; non-updatable
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Database timestamp marking when the user profile was last updated
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Relationships (not eagerly loaded — accessed via service)
    // Bidirectional one-to-one relationship with JobSeekerProfile, cascaded, fetched lazily
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JobSeekerProfile jobSeeker;

    // Bidirectional one-to-one relationship with RecruiterProfile, cascaded, fetched lazily
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RecruiterProfile recruiter;
}
