package com.fairhire.services.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 4B: Deterministic Scoring Engine Unit Tests")
class DeterministicScoringEngineTest {

    private DeterministicScoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DeterministicScoringEngine();
    }

    @Test
    @DisplayName("Oracle 1: Perfect Match - All required and preferred matched (100.00)")
    void testPerfectMatch() {
        Set<Long> required = Set.of(101L, 102L); // Java, Spring Boot
        Set<Long> preferred = Set.of(201L);      // Docker
        Set<Long> candidate = Set.of(101L, 102L, 201L, 999L); // Candidate has all plus extra

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        assertEquals(0, new BigDecimal("1.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("100.00").compareTo(result.overallScore()));
        assertEquals(2, result.requiredSkillsTotal());
        assertEquals(2, result.requiredSkillsMatched());
        assertEquals(1, result.preferredSkillsTotal());
        assertEquals(1, result.preferredSkillsMatched());
    }

    @Test
    @DisplayName("Oracle 2: Required Only - No preferred skills defined (100.00)")
    void testRequiredOnlyPerfect() {
        Set<Long> required = Set.of(101L, 102L);
        Set<Long> preferred = Collections.emptySet();
        Set<Long> candidate = Set.of(101L, 102L);

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        assertEquals(0, new BigDecimal("1.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("100.00").compareTo(result.overallScore()));
        assertEquals(2, result.requiredSkillsTotal());
        assertEquals(2, result.requiredSkillsMatched());
        assertEquals(0, result.preferredSkillsTotal());
        assertEquals(0, result.preferredSkillsMatched());
    }

    @Test
    @DisplayName("Oracle 3: Partial Required - 1 of 2 matched (50.00)")
    void testPartialRequired() {
        Set<Long> required = Set.of(101L, 102L);
        Set<Long> preferred = Collections.emptySet();
        Set<Long> candidate = Set.of(101L);

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        assertEquals(0, new BigDecimal("0.5000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("50.00").compareTo(result.overallScore()));
        assertEquals(2, result.requiredSkillsTotal());
        assertEquals(1, result.requiredSkillsMatched());
    }

    @Test
    @DisplayName("Oracle 4: Standard Balanced - 80% required + 20% preferred (90.00)")
    void testStandardBalancedMatch() {
        Set<Long> required = Set.of(101L); // Java
        Set<Long> preferred = Set.of(201L, 202L); // Docker, AWS
        Set<Long> candidate = Set.of(101L, 201L); // Java, Docker

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        // Req: 1/1 = 1.0000. Pref: 1/2 = 0.5000.
        // Overall: 0.80 * 1.0000 + 0.20 * 0.5000 = 0.8000 + 0.1000 = 0.9000 -> 90.00
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.5000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("90.00").compareTo(result.overallScore()));
        assertEquals(1, result.requiredSkillsTotal());
        assertEquals(1, result.requiredSkillsMatched());
        assertEquals(2, result.preferredSkillsTotal());
        assertEquals(1, result.preferredSkillsMatched());
    }

    @Test
    @DisplayName("Oracle 5: Preferred Only - Mathematical evaluation (50.00)")
    void testPreferredOnly() {
        Set<Long> required = Collections.emptySet();
        Set<Long> preferred = Set.of(201L, 202L); // Docker, Kubernetes
        Set<Long> candidate = Set.of(201L);

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        assertEquals(0, new BigDecimal("0.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.5000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("50.00").compareTo(result.overallScore()));
    }

    @Test
    @DisplayName("Oracle 6: Zero Requirement Job - Aborts with MatchingRequirementsNotFoundException")
    void testZeroRequirementJobRejected() {
        Set<Long> required = Collections.emptySet();
        Set<Long> preferred = Collections.emptySet();
        Set<Long> candidate = Set.of(101L, 201L);

        assertThrows(MatchingRequirementsNotFoundException.class, () -> {
            engine.computeScore(candidate, required, preferred);
        });
    }

    @Test
    @DisplayName("Oracle 7 & 12: Required / Preferred Overlap - REQUIRED takes precedence")
    void testOverlapPrecedence() {
        Set<Long> required = Set.of(101L); // Java
        Set<Long> preferred = Set.of(101L, 201L); // Java (overlap), Docker
        Set<Long> candidate = Set.of(101L); // Java only

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        // Overlap: 101L purged from preferred. Preferred now only has 201L.
        assertEquals(1, result.requiredSkillsTotal());
        assertEquals(1, result.requiredSkillsMatched());
        assertEquals(1, result.preferredSkillsTotal());
        assertEquals(0, result.preferredSkillsMatched());

        // Req: 1.0000, Pref: 0.0000.
        // Overall: 0.80 * 1.0000 + 0.20 * 0.0000 = 0.8000 -> 80.00
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("80.00").compareTo(result.overallScore()));
    }

    @Test
    @DisplayName("Oracle 8 & 9: Zero Candidate Skills - 0.00 score and 0 coverage")
    void testZeroCandidateSkills() {
        Set<Long> required = Set.of(101L, 102L);
        Set<Long> preferred = Set.of(201L);
        Set<Long> candidate = Collections.emptySet();

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        assertEquals(0, new BigDecimal("0.0000").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(result.preferredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.00").compareTo(result.overallScore()));
        assertEquals(0, result.requiredSkillsMatched());
        assertEquals(0, result.preferredSkillsMatched());
    }

    @Test
    @DisplayName("Oracle 10: Determinism - Repeated calls yield identical results")
    void testDeterminism() {
        Set<Long> required = Set.of(101L, 102L, 103L);
        Set<Long> preferred = Set.of(201L, 202L);
        Set<Long> candidate = Set.of(101L, 201L);

        DeterministicScoringEngine.MatchScoreResult first = engine.computeScore(candidate, required, preferred);

        for (int i = 0; i < 100; i++) {
            DeterministicScoringEngine.MatchScoreResult next = engine.computeScore(candidate, required, preferred);
            assertEquals(first.overallScore(), next.overallScore());
            assertEquals(first.requiredSkillCoverage(), next.requiredSkillCoverage());
            assertEquals(first.preferredSkillCoverage(), next.preferredSkillCoverage());
            assertEquals(first.requiredSkillsMatched(), next.requiredSkillsMatched());
            assertEquals(first.preferredSkillsMatched(), next.preferredSkillsMatched());
        }
    }

    @Test
    @DisplayName("Oracle 17: Score Precision - Repeating fractions round with HALF_UP (2/3 = 0.6667 -> 66.67)")
    void testRepeatingFractionsPrecision() {
        Set<Long> required = Set.of(101L, 102L, 103L); // 3 skills
        Set<Long> preferred = Collections.emptySet();
        Set<Long> candidate = Set.of(101L, 102L);      // 2 matched

        DeterministicScoringEngine.MatchScoreResult result = engine.computeScore(candidate, required, preferred);

        // 2 / 3 = 0.666666... -> rounded HALF_UP to scale 4 = 0.6667
        // overallScore = 0.6667 * 100 = 66.67
        assertEquals(0, new BigDecimal("0.6667").compareTo(result.requiredSkillCoverage()));
        assertEquals(0, new BigDecimal("66.67").compareTo(result.overallScore()));
    }
}
