"""
Blind Screening Module
-------------------------
Produces an anonymised version of a resume by stripping the candidate's
name and neutralising gender-indicating pronouns/titles, so that the
SAME matching engine (matcher.py) can be re-run on identity-free input.
Comparing scores before vs. after anonymisation is what lets FairHire AI
reproduce, inside a live system, the effect that Aslund & Skans (2012)
observed in real-world blind recruitment.
"""

import re
from config import GENDER_PRONOUNS, IDENTITY_FIELDS_TO_STRIP


def anonymize_resume(resume_text, candidate_name):
    """
    Returns (anonymized_text, list_of_redactions).
    Strips the candidate's name (wherever it appears) and neutralises
    gendered pronouns/titles. Non-destructive to skills/experience content.
    """
    text = resume_text
    redactions = []

    # 1. Remove the candidate's name wherever it appears.
    if candidate_name and candidate_name != "Unknown Candidate":
        name_pattern = re.compile(re.escape(candidate_name), re.IGNORECASE)
        if name_pattern.search(text):
            text = name_pattern.sub("[CANDIDATE]", text)
            redactions.append(f"Name '{candidate_name}' -> [CANDIDATE]")

    # 2. Neutralise gendered pronouns / titles.
    for word, neutral in GENDER_PRONOUNS.items():
        pattern = re.compile(r"(?<![a-zA-Z])" + re.escape(word) + r"(?![a-zA-Z])",
                              re.IGNORECASE)
        if pattern.search(text):
            text, n = pattern.subn(neutral, text)
            if n:
                redactions.append(f"'{word}' -> '{neutral}' ({n}x)")

    # 3. Redact Email addresses
    email_pattern = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
    if email_pattern.search(text):
        text, n = email_pattern.subn("[EMAIL_REDACTED]", text)
        if n:
            redactions.append(f"Redacted {n} email address(es)")

    # 4. Redact Phone numbers
    phone_pattern = re.compile(r"(\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b")
    if phone_pattern.search(text):
        text, n = phone_pattern.subn("[PHONE_REDACTED]", text)
        if n:
            redactions.append(f"Redacted {n} phone number(s)")

    # 5. Strip explicit identity-field lines (e.g. "Gender: Female").
    lines = text.splitlines()
    cleaned_lines = []
    for line in lines:
        stripped_line = line.strip().lower()
        if any(stripped_line.startswith(field + ":") for field in IDENTITY_FIELDS_TO_STRIP):
            redactions.append(f"Removed identity field line: '{line.strip()}'")
            continue
        cleaned_lines.append(line)

    return "\n".join(cleaned_lines), redactions
