export interface AgentQueryRequest {
  query: string;
  limit?: number;
}

export interface SupportingEvidence {
  agent: string;
  type: string;
  summary?: string;
  data: any;
}

export interface AgentQueryResponse {
  answer: string;
  agentsUsed: string[];
  supportingEvidence: SupportingEvidence[];
  executionPath?: string[];
}
