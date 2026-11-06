from typing import List, Optional
from uuid import UUID
from pydantic import BaseModel, Field


class GenerateEmbeddingRequest(BaseModel):
    text: str = Field(min_length=1, description="Feedback text or content to embed")
    task_type: Optional[str] = Field(default="RETRIEVAL_DOCUMENT", description="Gemini task type")
    dimension: Optional[int] = Field(default=768, ge=64, le=3072, description="Target embedding dimension")


class QueryEmbeddingRequest(BaseModel):
    query: str = Field(min_length=1, description="User search query to embed")
    dimension: Optional[int] = Field(default=768, ge=64, le=3072, description="Target embedding dimension")


class EmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embedding: List[float]


class BatchEmbeddingItemRequest(BaseModel):
    id: UUID
    text: str = Field(min_length=1)


class BatchGenerateEmbeddingRequest(BaseModel):
    items: List[BatchEmbeddingItemRequest] = Field(max_length=100)
    dimension: Optional[int] = Field(default=768, ge=64, le=3072)


class BatchEmbeddingItemResponse(BaseModel):
    id: UUID
    model: str
    dimension: int
    embedding: List[float]


class BatchEmbeddingResponse(BaseModel):
    total: int
    successful: int
    failed: int
    results: List[BatchEmbeddingItemResponse]
