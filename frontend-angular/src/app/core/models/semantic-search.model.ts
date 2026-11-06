export interface SemanticSearchRequest {
  query: string;
  limit?: number;
}

export interface SemanticSearchResult {
  feedbackId: string;
  feedbackText: string;
  similarity: number;
  category: string;
  theme: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}
