export interface FeedbackEmbedding {
  embeddingId: string;
  feedbackId: string;
  analysisId: string;
  embeddingModel: string;
  embeddingDimension: number;
  isCurrent: boolean;
  createdAt: string;
}

export interface BatchEmbeddingSummary {
  batchId: string;
  totalRequested: number;
  generatedCount: number;
  skippedCount: number;
  failedCount: number;
  results: FeedbackEmbedding[];
}
