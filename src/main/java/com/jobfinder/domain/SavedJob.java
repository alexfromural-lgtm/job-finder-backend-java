package com.jobfinder.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapped from Prisma model SavedJob.
 * Unique constraint on (job_id, job_seeker_id) matches Prisma @@unique([jobId, jobSeekerId]).
 */
@Entity
@Table(
    name = "saved_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_saved_job_seeker",
        columnNames = {"job_id", "job_seeker_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_seeker_id", nullable = false)
    private JobSeekerProfile jobSeeker;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant savedAt;
}
