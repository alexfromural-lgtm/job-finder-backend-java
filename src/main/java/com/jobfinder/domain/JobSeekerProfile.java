package com.jobfinder.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapped from Prisma model JobSeekerProfile.
 * skills → @ElementCollection (String list in a join table).
 */
@Entity
@Table(name = "job_seeker_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSeekerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_seeker_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private String education;

    @Column(columnDefinition = "TEXT")
    private String experience;

    private String resumeUrl;
}
