export type ProcessingStatus = 'STORED' | 'DUPLICATE' | 'FAILED';

export interface FeedbackItem {
  feedbackId: string;
  batchId: string;
  feedbackDate: string;
  channel: string;
  product: string;
  rating?: number;
  customerSegment?: string;
  region?: string;
  feedbackText: string;
  originalRowNumber: number;
  processingStatus: ProcessingStatus;
  createdAt: string;
}
