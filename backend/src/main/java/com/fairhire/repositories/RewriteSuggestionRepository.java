package com.fairhire.repositories;

import com.fairhire.models.RewriteSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewriteSuggestionRepository extends JpaRepository<RewriteSuggestion, Long> {
    List<RewriteSuggestion> findByBiasReportId(Long biasReportId);
}
