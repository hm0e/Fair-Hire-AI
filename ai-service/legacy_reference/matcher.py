"""
Candidate-Job Matching Engine
-------------------------------
Implements THREE matching strategies on the SAME input pair so they can
be directly compared, which is the core novelty of FairHire AI:

  1. keyword_score()   - classic ATS-style lexical / TF-IDF overlap
  2. semantic_score()  - transformer sentence-embedding similarity
                          (Sentence-BERT), falling back to TF-IDF if the
                          sentence-transformers package / model weights
                          are not available in the current environment
                          (e.g. no internet access to download weights).
  3. skill_overlap()   - direct skill-set Jaccard overlap, used as a
                          simple interpretable signal alongside the two
                          text-similarity scores.

A single get_all_scores() call returns all three so the comparison
dashboard can show them side by side for every resume.
"""

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

_SBERT_MODEL = None
_SBERT_AVAILABLE = None  # tri-state: None = not checked yet


def _try_load_sbert():
    """Lazily attempt to load a Sentence-BERT model. Cached after first try."""
    global _SBERT_MODEL, _SBERT_AVAILABLE
    if _SBERT_AVAILABLE is not None:
        return _SBERT_AVAILABLE

    try:
        from sentence_transformers import SentenceTransformer
        _SBERT_MODEL = SentenceTransformer("all-MiniLM-L6-v2")
        _SBERT_AVAILABLE = True
    except Exception:
        # No internet access / package missing / model download failed.
        # Fall back to TF-IDF-based similarity so the app still runs.
        _SBERT_AVAILABLE = False
    return _SBERT_AVAILABLE


def tfidf_score(text_a, text_b):
    """TF-IDF cosine similarity between two texts -> float in [0, 1]."""
    if not text_a.strip() or not text_b.strip():
        return 0.0
    vectorizer = TfidfVectorizer(stop_words="english")
    try:
        tfidf = vectorizer.fit_transform([text_a, text_b])
    except ValueError:
        return 0.0
    sim = cosine_similarity(tfidf[0:1], tfidf[1:2])[0][0]
    return round(float(sim), 4)


def keyword_score(jd_skills, resume_skills):
    """
    Classic ATS-style score: fraction of JD-required skills found
    verbatim in the resume's extracted skill list.
    """
    if not jd_skills:
        return 0.0
    jd_set, resume_set = set(jd_skills), set(resume_skills)
    overlap = jd_set & resume_set
    return round(len(overlap) / len(jd_set), 4)


def skill_jaccard(jd_skills, resume_skills):
    """Jaccard similarity between JD and resume skill sets."""
    jd_set, resume_set = set(jd_skills), set(resume_skills)
    if not jd_set and not resume_set:
        return 0.0
    union = jd_set | resume_set
    if not union:
        return 0.0
    return round(len(jd_set & resume_set) / len(union), 4)


def semantic_score(jd_text, resume_text):
    """
    Sentence-embedding cosine similarity. Uses Sentence-BERT when
    available; otherwise transparently falls back to TF-IDF so the
    pipeline never breaks (fallback is flagged in the return dict by
    the caller via `semantic_backend`).
    """
    if _try_load_sbert():
        embeddings = _SBERT_MODEL.encode([jd_text, resume_text])
        sim = cosine_similarity([embeddings[0]], [embeddings[1]])[0][0]
        return round(float(sim), 4), "sentence-bert (all-MiniLM-L6-v2)"
    else:
        return tfidf_score(jd_text, resume_text), "tfidf-fallback"


def get_all_scores(jd_text, jd_skills, resume_text, resume_skills):
    """Compute every matching signal for one (JD, resume) pair."""
    kw = keyword_score(jd_skills, resume_skills)
    jaccard = skill_jaccard(jd_skills, resume_skills)
    sem, backend = semantic_score(jd_text, resume_text)
    return {
        "keyword_score": kw,
        "skill_jaccard": jaccard,
        "semantic_score": sem,
        "semantic_backend": backend,
    }
