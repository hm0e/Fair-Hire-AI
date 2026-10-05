package com.fairhire.repositories;

import com.fairhire.models.BiasReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BiasReportRepository extends JpaRepository<BiasReport, Long> {
    List<BiasReport> findByJobId(Long jobId);
}
