export interface RowValidationError {
  rowNumber: number;
  field: string;
  message: string;
  rawRecord: string;
}

export interface ValidateCsvResponse {
  valid: boolean;
  rowCount: number;
  validRows: number;
  invalidRows: number;
  headerValidation: boolean;
  sampleErrors: RowValidationError[];
}
