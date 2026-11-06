export interface HealthResponse {
  status: string;
  service: string;
  version: string;
  timestamp: string;
  details?: Record<string, unknown>;
}
