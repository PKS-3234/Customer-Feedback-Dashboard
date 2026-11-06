# Customer Feedback Intelligence Platform - AI Service

FastAPI Python service for AI-powered customer feedback intelligence.

## Tech Stack
- Python 3.12
- FastAPI
- Pydantic v2 & Pydantic-Settings
- Pytest & HTTPX

## Setup & Running

```bash
python -m venv .venv
# On Windows
.venv\Scripts\activate
# On Linux/macOS
# source .venv/bin/activate

pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Running Tests

```bash
pytest tests/
```
