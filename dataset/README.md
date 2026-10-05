# FairHire AI — Dataset Directory

This directory stores datasets used for fair recruitment research, benchmarking, and evaluation.

## Directory Structure
- `raw/`: Unmodified input resumes (PDF/DOCX/TXT) and raw job descriptions.
- `processed/`: Structured, tokenized, and normalized representations.
- `annotations/`: Human-labeled ground truth for bias detection and candidate-JD relevance.
- `schemas/`: JSON schemas describing dataset record formats.

## Reproducibility Rules
- Never modify raw files in place.
- Every experimental dataset must have an associated `dataset_version.json` metadata record.
