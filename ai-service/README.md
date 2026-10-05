# FairHire AI — Python AI / NLP Service

Dedicated microservice for Natural Language Processing, resume parsing, skill extraction, Sentence-BERT semantic matching, exclusionary language detection, and inclusive rewrite generation.

## 🚀 Quickstart

### Prerequisites
- Python 3.10+ (Python 3.14 supported)
- Virtual environment recommended

### Installation & Run
```bash
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

The service runs at `http://localhost:5000`.
- OpenAPI Documentation: `http://localhost:5000/docs`
- Health Endpoint: `GET http://localhost:5000/api/v1/health`

### Running Automated Tests
```bash
pytest tests/ -v
```
