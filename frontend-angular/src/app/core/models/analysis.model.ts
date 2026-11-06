export interface FeedbackAnalysis {
  analysisId: string;
  feedbackId: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  category: string;
  theme: string;
  emotion: 'Happy' | 'Frustrated' | 'Angry' | 'Concerned' | 'Neutral';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  department: string;
  shortSummary: string;
  confidenceScore: number;
  modelName: string;
  modelVersion: string;
  isCurrent: boolean;
  createdAt: string;
}

export interface BatchAnalysisSummary {
  batchId: string;
  totalRequested: number;
  analyzedCount: number;
  skippedCount: number;
  failedCount: number;
  results: FeedbackAnalysis[];
}
