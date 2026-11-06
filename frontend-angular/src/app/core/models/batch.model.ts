export type BatchStatus = 'PROCESSING' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED';

export interface BatchSummary {
  batchId: string;
  sourceFileName: string;
  sourceSystem: string;
  uploadUser: string;
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  duplicateRows: number;
  status: BatchStatus;
  createdAt: string;
  completedAt?: string;
}

export interface BatchDetail extends BatchSummary {
  fileHash: string;
}
