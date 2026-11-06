from fastapi import APIRouter, HTTPException, status
from app.models.analysis import (
    AnalyzeFeedbackRequest,
    AnalysisResponse,
    BatchAnalyzeRequest,
    BatchAnalyzeResponse,
)
from app.service.analysis_service import AnalysisService

router = APIRouter()
analysis_service = AnalysisService()


@router.post(
    "/analyze",
    response_model=AnalysisResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze single customer feedback record using Gemini",
)
def analyze_feedback(request: AnalyzeFeedbackRequest) -> AnalysisResponse:
    try:
        return analysis_service.analyze_single(request)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Analysis failed: {str(e)}",
        )


@router.post(
    "/analyze/batch",
    response_model=BatchAnalyzeResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze multiple customer feedback records using Gemini",
)
def analyze_batch(request: BatchAnalyzeRequest) -> BatchAnalyzeResponse:
    try:
        return analysis_service.analyze_batch(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch analysis failed: {str(e)}",
        )
