package com.fairhire.repositories;

import com.fairhire.models.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByEmail(String email);
    Optional<Candidate> findByFullName(String fullName);

    default Optional<Candidate> findByName(String name) {
        return findByFullName(name);
    }
}
