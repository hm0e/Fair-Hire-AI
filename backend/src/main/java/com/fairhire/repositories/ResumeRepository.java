package com.fairhire.repositories;

import com.fairhire.models.Resume;
import com.fairhire.models.enums.ParsingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByOrderByIdDesc();
    Optional<Resume> findByFileHashSha256(String fileHashSha256);
    boolean existsByFileHashSha256(String fileHashSha256);
    List<Resume> findByCandidateId(Long candidateId);

    @org.springframework.transaction.annotation.Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Resume r SET r.parsingStatus = :status, r.parsingError = :error, r.updatedAt = :updatedAt WHERE r.id = :id")
    void updateParsingStatusAndError(
            @Param("id") Long id,
            @Param("status") ParsingStatus status,
            @Param("error") String error,
            @Param("updatedAt") Instant updatedAt
    );
}
