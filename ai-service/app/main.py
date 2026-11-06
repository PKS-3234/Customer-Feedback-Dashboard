from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.endpoints.health import router as health_router
from app.api.v1.endpoints.analysis import router as analysis_router
from app.api.v1.endpoints.embedding import router as embedding_router
from app.api.v1.endpoints.agent import router as agent_router
from app.api.v1.router import api_v1_router
from app.core.config import get_settings

settings = get_settings()

app = FastAPI(
    title="Customer Feedback Intelligence Platform - AI Service",
    description="FastAPI service for feedback processing, embeddings, and agentic workflows.",
    version=settings.APP_VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Root /health endpoint
app.include_router(health_router, prefix="", tags=["Health"])

# API v1 prefix (/api/v1/health)
app.include_router(api_v1_router, prefix="/api/v1")

# Internal v1 endpoints (/internal/v1/analyze, /internal/v1/embeddings, /internal/v1/agents)
app.include_router(analysis_router, prefix="/internal/v1", tags=["Internal Analysis"])
app.include_router(embedding_router)
app.include_router(agent_router)


@app.get("/", tags=["Root"])
def root():
    return {
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "UP",
        "docs": "/docs",
        "health": "/health",
        "api_v1_health": "/api/v1/health"
    }
