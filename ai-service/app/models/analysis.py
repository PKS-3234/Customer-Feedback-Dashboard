from typing import Literal, Optional, List
from uuid import UUID
from pydantic import BaseModel, Field


class AnalyzeFeedbackRequest(BaseModel):
    feedback_id: UUID
    feedback_text: str
    channel: Optional[str] = None
    product: Optional[str] = None
    rating: Optional[int] = None
    customer_segment: Optional[str] = None
    region: Optional[str] = None


class FeedbackAnalysisDTO(BaseModel):
    sentiment: Literal["POSITIVE", "NEGATIVE", "NEUTRAL"]
    category: str = Field(description="Operational category (e.g., Payments, Authentication, Account Services, Transfers, Mobile App, Customer Support, Cards)")
    theme: str = Field(description="Short business topic (e.g., OTP Issue, Duplicate Charge, Login Failure, Slow Transfer)")
    emotion: Literal["Happy", "Frustrated", "Angry", "Concerned", "Neutral"]
    priority: Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    department: str = Field(description="Resolving team (e.g., Payments Team, Digital Banking, Support Operations, Customer Service)")
    short_summary: str = Field(description="One sentence factual summary")
    confidence_score: float = Field(ge=0.0, le=1.0, description="Confidence score between 0.0 and 1.0")
    model_name: Optional[str] = None
    model_version: Optional[str] = None


class AnalysisResponse(FeedbackAnalysisDTO):
    feedback_id: UUID


class BatchAnalyzeRequest(BaseModel):
    items: List[AnalyzeFeedbackRequest]


class BatchAnalyzeResponse(BaseModel):
    total: int
    successful: int
    failed: int
    results: List[AnalysisResponse]
