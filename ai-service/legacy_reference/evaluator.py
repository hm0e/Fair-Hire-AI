"""
FairHire Evaluator
--------------------
Runs keyword matching, semantic matching, and blind (anonymised)
semantic matching on the SAME job description and the SAME pool of
resumes, then reports:

  - per-candidate scores under all three modes, plus a per-candidate
    skill-gap report (which JD-required skills each candidate lacks)
  - resulting RANKINGS under all three modes, and a Spearman rank
    correlation between the semantic and blind-semantic rankings
    (1.0 = identical ranking, lower = screening identity changed
    outcomes more)
  - score distribution statistics (mean / std dev) per mode, so a
    JD that fails to meaningfully separate candidates can be spotted
  - the JD's bias score (gender + age + readability) before and after
    inclusive rewriting
  - a single composite Fairness Index (0-100) combining rank-shift,
    bias reduction potential, and the JD's readability

No prior work in the literature review runs all of this together on one
system - that comparison IS the novelty claim.
"""

import statistics
from modules import matcher, blind_screen, bias_detector


def rank_candidates(scored_candidates, score_key):
    """Return candidates sorted descending by score_key, with rank added."""
    ranked = sorted(scored_candidates, key=lambda c: c[score_key], reverse=True)
    for i, c in enumerate(ranked, start=1):
        c[f"rank_{score_key}"] = i
    return ranked


def spearman_correlation(ranks_a, ranks_b):
    """
    Spearman rank correlation coefficient between two equal-length rank
    lists (no external dependency). Returns a value in [-1, 1]; 1.0
    means the two rankings are identical.
    """
    n = len(ranks_a)
    if n < 2:
        return 1.0
    d_squared_sum = sum((a - b) ** 2 for a, b in zip(ranks_a, ranks_b))
    rho = 1 - (6 * d_squared_sum) / (n * (n ** 2 - 1))
    return round(rho, 4)


def compute_skill_gap(jd_skills, resume_skills):
    """JD-required skills the candidate's resume does NOT contain."""
    return sorted(set(jd_skills) - set(resume_skills))


def score_distribution(candidates, key):
    values = [c[key] for c in candidates]
    if not values:
        return {"mean": 0.0, "stdev": 0.0, "min": 0.0, "max": 0.0}
    return {
        "mean": round(statistics.mean(values), 4),
        "stdev": round(statistics.pstdev(values), 4) if len(values) > 1 else 0.0,
        "min": round(min(values), 4),
        "max": round(max(values), 4),
    }


def run_full_comparison(jd_text, jd_skills, resumes):
    """
    resumes: list of dicts, each from resume_parser.parse_resume(), i.e.
             {name, raw_text, skills, years_experience}

    Returns a dict with per-candidate scores/ranks under each mode, JD
    bias analysis, and aggregate fairness + distribution metrics.
    """
    candidates = []

    for r in resumes:
        base_scores = matcher.get_all_scores(jd_text, jd_skills, r["raw_text"], r["skills"])

        anon_text, redactions = blind_screen.anonymize_resume(r["raw_text"], r["name"])
        blind_sem_score, blind_backend = matcher.semantic_score(jd_text, anon_text)

        candidates.append({
            "name": r["name"],
            "years_experience": r["years_experience"],
            "skills": r["skills"],
            "skill_gap": compute_skill_gap(jd_skills, r["skills"]),
            "keyword_score": base_scores["keyword_score"],
            "skill_jaccard": base_scores["skill_jaccard"],
            "semantic_score": base_scores["semantic_score"],
            "semantic_backend": base_scores["semantic_backend"],
            "blind_semantic_score": blind_sem_score,
            "blind_score": blind_sem_score,
            "blind_backend": blind_backend,
            "redactions_applied": redactions,
            "raw_text": r.get("raw_text", ""),
            "anonymized_text": anon_text,
        })

    # Rank under each mode independently.
    by_keyword = rank_candidates([dict(c) for c in candidates], "keyword_score")
    by_semantic = rank_candidates([dict(c) for c in candidates], "semantic_score")
    by_blind = rank_candidates([dict(c) for c in candidates], "blind_semantic_score")

    rank_map_kw = {c["name"]: c["rank_keyword_score"] for c in by_keyword}
    rank_map_sem = {c["name"]: c["rank_semantic_score"] for c in by_semantic}
    rank_map_blind = {c["name"]: c["rank_blind_semantic_score"] for c in by_blind}

    rank_shift_total = 0
    for c in candidates:
        c["rank_keyword"] = rank_map_kw[c["name"]]
        c["rank_keyword_score"] = c["rank_keyword"]
        c["rank_semantic"] = rank_map_sem[c["name"]]
        c["rank_semantic_score"] = c["rank_semantic"]
        c["rank_blind"] = rank_map_blind[c["name"]]
        c["rank_blind_score"] = c["rank_blind"]
        c["rank_blind_semantic_score"] = c["rank_blind"]
        c["rank_shift_semantic_vs_blind"] = abs(c["rank_semantic"] - c["rank_blind"])
        rank_shift_total += c["rank_shift_semantic_vs_blind"]

    # Sort the master list by semantic score by default for display.
    candidates.sort(key=lambda c: c["semantic_score"], reverse=True)

    # Spearman correlation needs two aligned rank lists in the same order.
    names_order = [c["name"] for c in candidates]
    sem_ranks = [rank_map_sem[n] for n in names_order]
    blind_ranks = [rank_map_blind[n] for n in names_order]
    rank_correlation = spearman_correlation(sem_ranks, blind_ranks)

    # JD-level bias analysis, before and after inclusive rewrite.
    bias_before = bias_detector.analyze_bias(jd_text)
    rewrite = bias_detector.rewrite_inclusive(jd_text)
    bias_after = bias_detector.analyze_bias(rewrite["rewritten_text"])

    avg_rank_shift = round(rank_shift_total / len(candidates), 2) if candidates else 0

    # ---- Composite Fairness Index (0-100) --------------------------------
    # Blends three signals into one headline number for the report:
    #   - rank stability between semantic & blind screening (40%)
    #   - how much bias the rewrite engine was able to remove (40%)
    #   - JD readability, since overly complex JDs narrow the pool (20%)
    stability_component = max(0.0, rank_correlation) * 40
    bias_component = min(1.0, (bias_before["bias_score"] - bias_after["bias_score"]) + 0.01) * 40 \
        if bias_before["bias_score"] > 0 else 40.0
    readability_score = bias_after["readability"]["flesch_score"]
    readability_component = max(0.0, min(1.0, readability_score / 100)) * 20
    fairness_index = round(stability_component + bias_component + readability_component, 1)

    return {
        "candidates": candidates,
        "jd_bias_before": bias_before,
        "jd_bias_after": bias_after,
        "jd_rewrite": rewrite,
        "score_distributions": {
            "keyword_score": score_distribution(candidates, "keyword_score"),
            "semantic_score": score_distribution(candidates, "semantic_score"),
            "blind_semantic_score": score_distribution(candidates, "blind_semantic_score"),
        },
        "fairness_summary": {
            "avg_rank_shift_semantic_vs_blind": avg_rank_shift,
            "candidates_with_rank_change": sum(
                1 for c in candidates if c["rank_shift_semantic_vs_blind"] > 0
            ),
            "total_candidates": len(candidates),
            "bias_reduction": round(
                bias_before["bias_score"] - bias_after["bias_score"], 4
            ),
            "rank_correlation_semantic_vs_blind": rank_correlation,
            "fairness_index": fairness_index,
        },
    }
