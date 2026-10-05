"""
Job-Description Bias Detection & Inclusive Rewrite Module
-------------------------------------------------------------
Scans a job description across THREE bias dimensions:
  1. Gendered language   (masculine-coded / feminine-coded wording)
  2. Age-coded language  (wording that implies a preferred age bracket)
  3. Readability         (overly complex JDs are known to discourage
                           candidates from non-traditional educational
                           backgrounds - a fairness-adjacent signal)

...and produces an automatically rewritten, more inclusive version of
the JD with a full change-log, so before/after bias scores can be
compared. This module is the Job-Description Bias Detection module
referenced in the FairHire AI research gap.
"""

import re
from config import MASCULINE_CODED_WORDS, FEMININE_CODED_WORDS, AGE_CODED_WORDS


def _find_flags(text, word_bank):
    text_lower = text.lower()
    flags = []
    for word in word_bank:
        pattern = r"(?<![a-zA-Z])" + re.escape(word) + r"(?![a-zA-Z])"
        for m in re.finditer(pattern, text_lower):
            start = max(0, m.start() - 30)
            end = min(len(text), m.end() + 30)
            flags.append({
                "word": word,
                "context": "..." + text[start:end].strip() + "...",
            })
    return flags


def analyze_gender_bias(jd_text):
    """Masculine vs. feminine coded language -> bias score + lean."""
    masc_flags = _find_flags(jd_text, MASCULINE_CODED_WORDS)
    fem_flags = _find_flags(jd_text, FEMININE_CODED_WORDS)

    masc_count = len(masc_flags)
    fem_count = len(fem_flags)
    total = masc_count + fem_count

    if total == 0:
        lean = "balanced / neutral"
        bias_score = 0.0
    else:
        # -1.0 = fully masculine-skewed, +1.0 = fully feminine-skewed
        raw_lean = (fem_count - masc_count) / total
        bias_score = round(abs(raw_lean), 4)
        if raw_lean > 0.15:
            lean = "feminine-skewed"
        elif raw_lean < -0.15:
            lean = "masculine-skewed"
        else:
            lean = "balanced / neutral"

    return {
        "bias_score": bias_score,          # 0.0 (neutral) - 1.0 (fully skewed)
        "lean": lean,
        "masculine_flags": masc_flags,
        "feminine_flags": fem_flags,
        "total_flagged_terms": total,
    }


def analyze_age_bias(jd_text):
    """Age-coded wording -> flagged terms + a 0-1 severity score."""
    flags = _find_flags(jd_text, AGE_CODED_WORDS)
    word_count = max(1, len(jd_text.split()))
    # Severity scales with density of flagged terms relative to JD length,
    # capped at 1.0.
    severity = round(min(1.0, len(flags) / (word_count / 40)), 4)
    return {
        "age_bias_score": severity,
        "flags": flags,
        "total_flagged_terms": len(flags),
    }


def _count_syllables(word):
    word = word.lower().strip(".,!?;:")
    if not word:
        return 0
    vowels = "aeiouy"
    count = 0
    prev_was_vowel = False
    for ch in word:
        is_vowel = ch in vowels
        if is_vowel and not prev_was_vowel:
            count += 1
        prev_was_vowel = is_vowel
    if word.endswith("e") and count > 1:
        count -= 1
    return max(1, count)


def compute_readability(jd_text):
    """
    Flesch Reading Ease, computed manually (no external dependency):
        206.835 - 1.015*(words/sentences) - 84.6*(syllables/words)
    Higher score = easier to read. Below ~50 is considered fairly hard
    to read, which research links to narrower applicant pools.
    """
    sentences = re.split(r"[.!?]+", jd_text)
    sentences = [s for s in sentences if s.strip()]
    words = re.findall(r"[A-Za-z']+", jd_text)

    if not sentences or not words:
        return {"flesch_score": 0.0, "level": "n/a"}

    syllables = sum(_count_syllables(w) for w in words)
    score = 206.835 - 1.015 * (len(words) / len(sentences)) - 84.6 * (syllables / len(words))
    score = round(score, 1)

    if score >= 70:
        level = "easy to read"
    elif score >= 50:
        level = "moderate"
    else:
        level = "difficult to read"

    return {"flesch_score": score, "level": level}


def analyze_bias(jd_text):
    """Full multi-dimensional bias report for a job description."""
    gender = analyze_gender_bias(jd_text)
    age = analyze_age_bias(jd_text)
    readability = compute_readability(jd_text)

    return {
        # Kept for backward compatibility with earlier dashboard fields:
        "bias_score": gender["bias_score"],
        "lean": gender["lean"],
        "masculine_flags": gender["masculine_flags"],
        "feminine_flags": gender["feminine_flags"],
        "total_flagged_terms": gender["total_flagged_terms"] + age["total_flagged_terms"],
        # New, richer breakdown:
        "gender_bias": gender,
        "age_bias": age,
        "readability": readability,
    }


def _fix_articles(text):
    """Fix 'a' vs 'an' mismatches after word replacement (e.g. 'an proactive' -> 'a proactive')."""
    def fix_an(m):
        art = "A" if m.group(1)[0].isupper() else "a"
        return f"{art} {m.group(2)}"

    def fix_a(m):
        art = "An" if m.group(1)[0].isupper() else "an"
        return f"{art} {m.group(2)}"

    text = re.sub(r"\b(an)\s+([b-df-hj-np-tv-zB-DF-HJ-NP-TV-Z]\w*)", fix_an, text, flags=re.IGNORECASE)
    text = re.sub(r"\b(a)\s+([aeioAEIO]\w*)", fix_a, text, flags=re.IGNORECASE)
    return text


def rewrite_inclusive(jd_text):
    """
    Produce an automatically rewritten version of the JD with flagged
    gendered AND age-coded terms swapped for neutral alternatives.
    Returns a change-log so the UI can show a before/after diff.
    """
    changes = []
    new_text = jd_text

    for bank in (MASCULINE_CODED_WORDS, FEMININE_CODED_WORDS, AGE_CODED_WORDS):
        for word, replacements in bank.items():
            pattern = re.compile(r"(?<![a-zA-Z])" + re.escape(word) + r"(?![a-zA-Z])",
                                  re.IGNORECASE)
            if pattern.search(new_text):
                replacement = replacements[0]
                new_text, n = pattern.subn(replacement, new_text)
                if n:
                    changes.append({"from": word, "to": replacement, "count": n})

    if changes:
        new_text = _fix_articles(new_text)

    return {
        "original_text": jd_text,
        "rewritten_text": new_text,
        "changes": changes,
    }

