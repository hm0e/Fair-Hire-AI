package com.fairhire.repositories;

import com.fairhire.models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
    List<Skill> findByIsActiveTrue();

    default Optional<Skill> findBySkillName(String skillName) {
        return findByName(skillName);
    }
}
