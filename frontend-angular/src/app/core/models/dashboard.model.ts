export interface DashboardKeyMetrics {
  totalFeedbackRecords: number;
  totalAnalyzedRecords: number;
  totalEmbeddedRecords: number;
  positiveFeedbackPercent: number;
  negativeFeedbackPercent: number;
  highPriorityFeedbackCount: number;
  currentBatches: number;
  processingErrors: number;
  failedRows: number;
  duplicateRows: number;
}

export interface ChartDataPoint {
  label: string;
  count: number;
  percentage: number;
}

export interface DashboardCharts {
  sentimentDistribution: ChartDataPoint[];
  categoryDistribution: ChartDataPoint[];
  themeDistribution: ChartDataPoint[];
  priorityDistribution: ChartDataPoint[];
  feedbackByChannel: ChartDataPoint[];
  feedbackByProduct: ChartDataPoint[];
  errorDistribution: ChartDataPoint[];
}

export interface SemanticSearchStatus {
  embeddingModel: string;
  embeddingDimension: number;
  totalEmbeddings: number;
  semanticSearchStatus: string;
  pgvectorStatus: string;
}

export interface DashboardResponse {
  keyMetrics: DashboardKeyMetrics;
  charts: DashboardCharts;
  semanticSearchStatus: SemanticSearchStatus;
  exampleQuestions: string[];
}

export interface AgentStatus {
  name: string;
  purpose: string;
  availability: string;
  executionHistory: string;
}
