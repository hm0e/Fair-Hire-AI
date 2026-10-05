"""
FairHire AI - Configuration & Static Knowledge Base
-----------------------------------------------------
Holds the skill taxonomy used for keyword extraction, and the
gendered-language word lists used for bias detection.

The gendered-word lists are adapted conceptually from the categories of
"masculine-coded" and "feminine-coded" language described in:
    Gaucher, D., Friesen, J., & Kay, A. C. (2011).
    "Evidence That Gendered Wording in Job Advertisements Exists and
    Sustains Gender Inequality."
This is FairHire AI's own compiled word bank for the purpose of this
research project, not a reproduction of the paper's dataset.
"""

# ---------------------------------------------------------------------
# Skill taxonomy used for resume / JD keyword & skill extraction.
# Extend this list freely - it drives both keyword matching and the
# "skills found" panel in the UI.
# ---------------------------------------------------------------------
_LANGUAGES = [
    "python", "java", "c++", "c", "c#", "javascript", "typescript", "go",
    "rust", "kotlin", "swift", "php", "ruby", "scala", "r", "matlab", "dart",
]
_WEB_FRAMEWORKS = [
    "django", "flask", "fastapi", "react", "angular", "vue.js", "node.js",
    "express.js", "next.js", "spring boot", "bootstrap", "jquery", "html",
    "css", "sass", "rest api", "graphql", "webpack",
]
_DATA_AND_DB = [
    "sql", "nosql", "mysql", "postgresql", "sqlite", "mongodb", "redis",
    "cassandra", "oracle db", "firebase", "data warehousing", "etl",
    "data modeling", "database design",
]
_DATA_SCIENCE_AI = [
    "pandas", "numpy", "seaborn", "matplotlib", "plotly", "scikit-learn",
    "tensorflow", "pytorch", "keras", "nlp", "natural language processing",
    "machine learning", "deep learning", "computer vision", "data analysis",
    "data visualization", "statistics", "predictive modeling",
    "feature engineering", "llm", "generative ai", "opencv", "big data",
    "hadoop", "spark", "power bi", "tableau", "excel", "data mining",
]
_CLOUD_DEVOPS = [
    "aws", "azure", "gcp", "docker", "kubernetes", "linux", "unix",
    "ci/cd", "jenkins", "terraform", "ansible", "nginx", "cloud computing",
    "microservices", "serverless", "devops",
]
_MOBILE = [
    "android", "ios", "react native", "flutter", "swift ui", "app development",
]
_TESTING_QA = [
    "unit testing", "test automation", "selenium", "pytest", "junit",
    "manual testing", "qa testing", "load testing", "postman",
]
_TOOLS_PRACTICES = [
    "git", "github", "gitlab", "jira", "confluence", "agile", "scrum",
    "kanban", "api development", "oop", "data structures", "algorithms",
    "system design", "version control", "web development",
]
_MARKETING_BUSINESS = [
    "seo", "digital marketing", "content marketing", "google analytics",
    "social media marketing", "business analysis", "market research",
    "crm", "salesforce",
]
_SOFT_SKILLS = [
    "communication", "leadership", "teamwork", "problem solving",
    "project management", "time management", "critical thinking",
    "adaptability", "collaboration", "presentation skills",
    "stakeholder management", "mentoring",
]

# ---------------------------------------------------------------------
# Skill taxonomy used for resume / JD keyword & skill extraction.
# Organised by category above, flattened here for matching. Extend any
# of the category lists freely - both drive keyword matching and the
# "skills found" panel in the UI.
# ---------------------------------------------------------------------
SKILL_TAXONOMY = sorted(set(
    _LANGUAGES + _WEB_FRAMEWORKS + _DATA_AND_DB + _DATA_SCIENCE_AI +
    _CLOUD_DEVOPS + _MOBILE + _TESTING_QA + _TOOLS_PRACTICES +
    _MARKETING_BUSINESS + _SOFT_SKILLS
))

# Category lookup so the UI/report can group a candidate's matched
# skills by type (e.g. "Languages", "Cloud & DevOps") instead of one
# flat list.
SKILL_CATEGORIES = {
    "Languages": _LANGUAGES,
    "Web Frameworks": _WEB_FRAMEWORKS,
    "Databases & Data Engineering": _DATA_AND_DB,
    "Data Science & AI": _DATA_SCIENCE_AI,
    "Cloud & DevOps": _CLOUD_DEVOPS,
    "Mobile": _MOBILE,
    "Testing & QA": _TESTING_QA,
    "Tools & Practices": _TOOLS_PRACTICES,
    "Marketing & Business": _MARKETING_BUSINESS,
    "Soft Skills": _SOFT_SKILLS,
}

# ---------------------------------------------------------------------
# Gendered-language word bank for Job Description Bias Detection.
# Each flagged word maps to one or more neutral replacement suggestions
# used by the Inclusive Rewrite Engine.
# ---------------------------------------------------------------------
MASCULINE_CODED_WORDS = {
    "aggressive": ["proactive", "driven"],
    "ambitious": ["motivated"],
    "assertive": ["confident"],
    "competitive": ["goal-oriented"],
    "dominant": ["leading"],
    "decisive": ["clear-thinking"],
    "independent": ["self-directed"],
    "individualistic": ["self-motivated"],
    "lead": ["guide", "coordinate"],
    "leader": ["coordinator", "team member"],
    "rockstar": ["skilled professional"],
    "ninja": ["expert"],
    "superior": ["strong"],
    "outspoken": ["communicative"],
    "self-reliant": ["resourceful"],
    "strong": ["capable"],
    "determined": ["dedicated"],
    "fearless": ["confident"],
    "headstrong": ["focused"],
    "dominate": ["excel in"],
    "confident": ["capable"],
    "adventurous": ["open to challenges"],
    "analytical": ["detail-oriented"],
    "challenging": ["engaging"],
    "driven": ["motivated"],
    "objective": ["fair-minded"],
    "outperform": ["succeed"],
    "principled": ["ethical"],
    "resilient": ["adaptable"],
    "self-sufficient": ["resourceful"],
    "superior product": ["high-quality product"],
    "world-class": ["highly skilled"],
}

FEMININE_CODED_WORDS = {
    "collaborative": ["cooperative", "team-oriented"],
    "compassionate": ["considerate"],
    "supportive": ["helpful"],
    "nurturing": ["mentoring"],
    "warm": ["approachable"],
    "loyal": ["committed"],
    "sensitive": ["attentive"],
    "understanding": ["empathetic"],
    "dependable": ["reliable"],
    "interpersonal": ["people-oriented"],
    "pleasant": ["friendly"],
    "trustworthy": ["reliable"],
    "connect": ["engage"],
    "honest": ["transparent"],
    "committed": ["dedicated"],
    "considerate": ["thoughtful"],
    "cooperative": ["team-oriented"],
    "empathetic": ["understanding of others"],
    "flexible": ["adaptable"],
    "gentle": ["approachable"],
    "kind": ["respectful"],
    "polite": ["professional"],
    "responsible": ["accountable"],
    "sincere": ["genuine"],
    "tactful": ["diplomatic"],
    "warmth": ["approachability"],
}

# ---------------------------------------------------------------------
# Age-coded language word bank (a second, distinct bias dimension from
# gendered wording). Terms in this list are commonly cited in
# age-discrimination-in-hiring research as signalling a preference for
# younger or older candidates, discouraging otherwise-qualified
# applicants outside the implied age bracket.
# ---------------------------------------------------------------------
AGE_CODED_WORDS = {
    "young": ["motivated"],
    "youthful": ["energetic"],
    "energetic": ["enthusiastic"],
    "digital native": ["digitally skilled"],
    "recent graduate": ["early-career candidate"],
    "fresh graduate": ["early-career candidate"],
    "fresher": ["early-career candidate"],
    "vibrant": ["dynamic"],
    "high-energy": ["motivated"],
    "mature": ["experienced"],
    "seasoned": ["experienced"],
    "veteran": ["experienced professional"],
    "years of experience required": ["relevant experience required"],
}

# Words that are gender-identifying and should be stripped/neutralised
# during Blind Screening.
GENDER_PRONOUNS = {
    "he": "they", "him": "them", "his": "their", "himself": "themself",
    "she": "they", "her": "them", "hers": "theirs", "herself": "themself",
    "mr": "candidate", "mr.": "candidate", "mrs": "candidate",
    "mrs.": "candidate", "ms": "candidate", "ms.": "candidate",
    "miss": "candidate",
}

# Fields removed entirely (not just neutralised) during blind screening,
# since they are strong identity signals rather than gendered language.
IDENTITY_FIELDS_TO_STRIP = [
    "name", "gender", "photo", "date of birth", "dob", "nationality",
    "marital status", "religion", "caste",
]

import os

_BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAMPLE_JD_PATH = os.path.join(_BASE_DIR, "data", "sample_jds.json")
SAMPLE_RESUME_DIR = os.path.join(_BASE_DIR, "data", "sample_resumes")

# Service Configuration
SERVICE_NAME = "fairhire-ai-service"
VERSION = "1.0.0"
HOST = os.getenv("AI_SERVICE_HOST", "0.0.0.0")
PORT = int(os.getenv("AI_SERVICE_PORT", "5000"))
DEBUG = os.getenv("DEBUG", "false").lower() == "true"
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "*").split(",")
MODEL_NAME = os.getenv("MODEL_NAME", "all-MiniLM-L6-v2")

