export interface ProcessingError {
  errorId: string;
  batchId: string;
  rowNumber?: number;
  rawRecord?: string;
  errorType: string;
  errorMessage: string;
  createdAt: string;
}
