package com.fairhire.services.matching;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure deterministic canonical skill matching and scoring engine.
 * Fully decoupled from candidate text, PII, ML models, and external services.
 * Implements the approved Phase 4 mathematical scoring specification with exact BigDecimal precision.
 */
@Component
public class DeterministicScoringEngine {

    private static final BigDecimal WEIGHT_REQUIRED = new BigDecimal("0.8000");
    private static final BigDecimal WEIGHT_PREFERRED = new BigDecimal("0.2000");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public record MatchScoreResult(
            BigDecimal requiredSkillCoverage,
            BigDecimal preferredSkillCoverage,
            BigDecimal overallScore,
            int requiredSkillsTotal,
            int requiredSkillsMatched,
            int preferredSkillsTotal,
            int preferredSkillsMatched,
            Set<Long> matchedRequiredSkillIds,
            Set<Long> unmatchedRequiredSkillIds,
            Set<Long> matchedPreferredSkillIds,
            Set<Long> unmatchedPreferredSkillIds
    ) {}

    /**
     * Compute canonical skill coverage and overall score for structured skill IDs.
     *
     * @param candidateSkillIds Extracted canonical skill IDs from candidate resume
     * @param requiredSkillIds  Explicit mandatory skill IDs from job requirements
     * @param preferredSkillIds Explicit optional/preferred skill IDs from job requirements
     * @return Deterministic MatchScoreResult with exact BigDecimal values
     */
    public MatchScoreResult computeScore(Set<Long> candidateSkillIds,
                                         Set<Long> requiredSkillIds,
                                         Set<Long> preferredSkillIds) {
        Set<Long> c = candidateSkillIds != null ? new HashSet<>(candidateSkillIds) : Collections.emptySet();
        Set<Long> r = requiredSkillIds != null ? new HashSet<>(requiredSkillIds) : Collections.emptySet();
        Set<Long> pRaw = preferredSkillIds != null ? new HashSet<>(preferredSkillIds) : Collections.emptySet();

        // Overlap resolution: REQUIRED takes precedence over PREFERRED
        Set<Long> p = new HashSet<>(pRaw);
        p.removeAll(r);

        // Zero-requirement check
        if (r.isEmpty() && p.isEmpty()) {
            throw new MatchingRequirementsNotFoundException("Job has zero recognized canonical skill requirements. Matching is unavailable.");
        }

        // Intersections
        Set<Long> mReq = new HashSet<>(r);
        mReq.retainAll(c);

        Set<Long> uReq = new HashSet<>(r);
        uReq.removeAll(c);

        Set<Long> mPref = new HashSet<>(p);
        mPref.retainAll(c);

        Set<Long> uPref = new HashSet<>(p);
        uPref.removeAll(c);

        int totalReq = r.size();
        int matchedReq = mReq.size();
        int totalPref = p.size();
        int matchedPref = mPref.size();

        // Coverage calculations with NUMERIC(5,4) precision
        BigDecimal covReq = totalReq > 0
                ? BigDecimal.valueOf(matchedReq).divide(BigDecimal.valueOf(totalReq), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal covPref = totalPref > 0
                ? BigDecimal.valueOf(matchedPref).divide(BigDecimal.valueOf(totalPref), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Score calculation with NUMERIC(5,2) precision
        BigDecimal overallScore;
        if (totalReq > 0 && totalPref > 0) {
            // Standard weighting: 80% required + 20% preferred
            BigDecimal rawScore = WEIGHT_REQUIRED.multiply(covReq).add(WEIGHT_PREFERRED.multiply(covPref));
            overallScore = rawScore.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
        } else if (totalReq > 0) {
            // Required-only weighting: 100% required
            overallScore = covReq.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Preferred-only weighting: 100% preferred (mathematical fallback)
            overallScore = covPref.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
        }

        return new MatchScoreResult(
                covReq,
                covPref,
                overallScore,
                totalReq,
                matchedReq,
                totalPref,
                matchedPref,
                Collections.unmodifiableSet(mReq),
                Collections.unmodifiableSet(uReq),
                Collections.unmodifiableSet(mPref),
                Collections.unmodifiableSet(uPref)
        );
    }
}
