"""
Resume Parser & Skill Extraction Module
----------------------------------------
Extracts raw text from PDF / DOCX / TXT resumes and pulls out a
normalised skill set using the SKILL_TAXONOMY, plus lightweight
heuristics for candidate name and years of experience.
"""

import os
import re
import io

try:
    import PyPDF2
except ImportError:
    PyPDF2 = None

try:
    import docx
except ImportError:
    docx = None

from config import SKILL_TAXONOMY


def extract_text_from_file(file_path):
    """Extract raw text from a .pdf, .docx, or .txt file."""
    ext = os.path.splitext(file_path)[1].lower()

    if ext == ".pdf":
        return _extract_pdf(file_path)
    elif ext == ".docx":
        return _extract_docx(file_path)
    else:  # .txt or unknown -> read as plain text
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()


def _extract_pdf(file_path):
    if PyPDF2 is None:
        raise RuntimeError("PyPDF2 is not installed. Run: pip install PyPDF2")
    text = []
    with open(file_path, "rb") as f:
        reader = PyPDF2.PdfReader(f)
        for page in reader.pages:
            text.append(page.extract_text() or "")
    return "\n".join(text)


def _extract_docx(file_path):
    if docx is None:
        raise RuntimeError("python-docx is not installed. Run: pip install python-docx")
    document = docx.Document(file_path)
    return "\n".join(p.text for p in document.paragraphs)


def extract_skills(text):
    """Return the subset of SKILL_TAXONOMY found in the given text."""
    text_lower = text.lower()
    found = []
    for skill in SKILL_TAXONOMY:
        # word-boundary-safe match, works for multi-word skills too
        pattern = r"(?<![a-zA-Z0-9])" + re.escape(skill) + r"(?![a-zA-Z0-9])"
        if re.search(pattern, text_lower):
            found.append(skill)
    return sorted(set(found))


def extract_candidate_name(text):
    """
    Heuristic: the candidate's name is usually the first non-empty line
    of the resume, provided it looks like a name (short, no digits,
    no email/phone markers).
    """
    for line in text.strip().splitlines():
        line = line.strip()
        if not line:
            continue
        if "@" in line or any(ch.isdigit() for ch in line):
            continue
        if len(line.split()) <= 5:
            return line
        break
    return "Unknown Candidate"


def extract_years_experience(text):
    """Robust regex heuristic for total years of experience."""
    text_lower = text.lower()
    years = []

    # 1. Direct mention: "2 years", "1.5+ years of experience", "3-5 years"
    matches = re.findall(r"(\d+(?:\.\d+)?)\s*(?:-\s*(\d+(?:\.\d+)?))?\s*\+?\s*years?", text_lower)
    for m in matches:
        if m[0]:
            try:
                years.append(float(m[0]))
            except ValueError:
                pass
        if m[1]:
            try:
                years.append(float(m[1]))
            except ValueError:
                pass

    # 2. Date ranges in experience section: (2024-Present), (2022 - 2024)
    current_year = 2026
    date_ranges = re.findall(r"\(?\b(20\d\d)\s*[-–to]+\s*(present|current|now|(?:20\d\d))\b\)?", text_lower)
    for start_str, end_str in date_ranges:
        try:
            start_yr = int(start_str)
            end_yr = current_year if end_str in ("present", "current", "now") else int(end_str)
            span = max(1.0, float(end_yr - start_yr))
            years.append(span)
        except ValueError:
            pass

    return max(years) if years else 0.0


def parse_resume(file_path):
    """Full pipeline: text -> {name, raw_text, skills, years_experience}."""
    text = extract_text_from_file(file_path)
    return {
        "name": extract_candidate_name(text),
        "raw_text": text,
        "skills": extract_skills(text),
        "years_experience": extract_years_experience(text),
    }
