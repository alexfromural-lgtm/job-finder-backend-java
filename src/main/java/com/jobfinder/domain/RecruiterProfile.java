package com.jobfinder.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Mapped from Prisma model RecruiterProfile.
 */
@Entity
@Table(name = "recruiter_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String companyName;

    private String companyWebsite;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String industry;
}
