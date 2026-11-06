import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BatchDetail, BatchSummary } from '../models/batch.model';
import { ValidateCsvResponse } from '../models/validation.model';
import { ProcessingError } from '../models/error.model';
import { FeedbackItem } from '../models/feedback.model';
import { PagedResponse } from '../models/pagination.model';

@Injectable({
  providedIn: 'root'
})
export class BatchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/batches`;
  private readonly feedbackUrl = `${environment.apiBaseUrl}/feedback`;

  validateCsv(file: File): Observable<ValidateCsvResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ValidateCsvResponse>(`${this.baseUrl}/validate`, formData);
  }

  uploadBatch(file: File, sourceSystem = 'WEB_UPLOAD', uploadUser = 'admin'): Observable<BatchDetail> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceSystem', sourceSystem);
    formData.append('uploadUser', uploadUser);
    return this.http.post<BatchDetail>(this.baseUrl, formData);
  }

  getBatches(page = 0, size = 10): Observable<PagedResponse<BatchSummary>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<BatchSummary>>(this.baseUrl, { params });
  }

  getBatchDetail(batchId: string): Observable<BatchDetail> {
    return this.http.get<BatchDetail>(`${this.baseUrl}/${batchId}`);
  }

  getBatchErrors(batchId: string, page = 0, size = 10): Observable<PagedResponse<ProcessingError>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<ProcessingError>>(`${this.baseUrl}/${batchId}/errors`, { params });
  }

  getFeedback(batchId?: string, page = 0, size = 20): Observable<PagedResponse<FeedbackItem>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (batchId) {
      params = params.set('batchId', batchId);
    }
    return this.http.get<PagedResponse<FeedbackItem>>(this.feedbackUrl, { params });
  }
}
