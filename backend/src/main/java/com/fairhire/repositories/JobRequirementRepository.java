package com.fairhire.repositories;

import com.fairhire.models.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, Long> {
    List<JobRequirement> findByJobId(Long jobId);
}
