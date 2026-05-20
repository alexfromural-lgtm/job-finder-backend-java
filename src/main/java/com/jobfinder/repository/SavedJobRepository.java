package com.jobfinder.repository;

import com.jobfinder.domain.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {

    Optional<SavedJob> findByJob_IdAndJobSeeker_Id(UUID jobId, UUID jobSeekerId);

    List<SavedJob> findByJobSeeker_IdOrderBySavedAtDesc(UUID jobSeekerId);
}
