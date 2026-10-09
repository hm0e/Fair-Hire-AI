package com.fairhire.repositories;

import com.fairhire.models.MatchSkillDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchSkillDetailRepository extends JpaRepository<MatchSkillDetail, Long> {

    List<MatchSkillDetail> findByMatchResultId(Long matchResultId);

    List<MatchSkillDetail> findByMatchResultIdOrderByJobSkillIdAsc(Long matchResultId);

    Optional<MatchSkillDetail> findByMatchResultIdAndJobSkillId(Long matchResultId, Long jobSkillId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM MatchSkillDetail d WHERE d.matchResult.id = :matchResultId")
    void deleteByMatchResultId(@org.springframework.data.repository.query.Param("matchResultId") Long matchResultId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM MatchSkillDetail d WHERE d.jobSkill.id = :jobSkillId")
    void deleteByJobSkillId(@org.springframework.data.repository.query.Param("jobSkillId") Long jobSkillId);
}
