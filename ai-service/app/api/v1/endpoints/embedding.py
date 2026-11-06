import logging
from fastapi import APIRouter, HTTPException, status

from app.models.embedding import (
    GenerateEmbeddingRequest,
    QueryEmbeddingRequest,
    EmbeddingResponse,
    BatchGenerateEmbeddingRequest,
    BatchEmbeddingResponse,
)
from app.workflow.embedding_workflow import EmbeddingWorkflow

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/internal/v1/embeddings", tags=["internal-embeddings"])
workflow = EmbeddingWorkflow()


@router.post(
    "/generate",
    response_model=EmbeddingResponse,
    summary="Generate vector embedding for a single text record",
)
def generate_embedding(request: GenerateEmbeddingRequest) -> EmbeddingResponse:
    try:
        return workflow.process_document_embedding(
            text=request.text,
            dimension=request.dimension,
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.error("Failed to generate embedding: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Embedding generation failed: {str(e)}",
        )


@router.post(
    "/query",
    response_model=EmbeddingResponse,
    summary="Generate query embedding vector for semantic search",
)
def generate_query_embedding(request: QueryEmbeddingRequest) -> EmbeddingResponse:
    try:
        return workflow.process_query_embedding(
            query=request.query,
            dimension=request.dimension,
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.error("Failed to generate query embedding: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Query embedding generation failed: {str(e)}",
        )


@router.post(
    "/batch-generate",
    response_model=BatchEmbeddingResponse,
    summary="Batch generate vector embeddings",
)
def batch_generate_embeddings(request: BatchGenerateEmbeddingRequest) -> BatchEmbeddingResponse:
    try:
        return workflow.service.generate_batch_embeddings(
            items=request.items,
            dimension=request.dimension,
        )
    except Exception as e:
        logger.error("Batch embedding generation failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch embedding generation failed: {str(e)}",
        )
