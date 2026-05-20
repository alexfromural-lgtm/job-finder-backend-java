package com.jobfinder.repository;

import com.jobfinder.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByJobIdAndJobSeekerId(UUID jobId, UUID jobSeekerId);

    List<Application> findByJobSeekerIdOrderByCreatedAtDesc(UUID jobSeekerId);

    List<Application> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}
