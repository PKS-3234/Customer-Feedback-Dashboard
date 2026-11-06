import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FeedbackEmbedding, BatchEmbeddingSummary } from '../models/embedding.model';

@Injectable({
  providedIn: 'root'
})
export class EmbeddingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/embeddings`;

  generateFeedbackEmbedding(feedbackId: string, force: boolean = false): Observable<FeedbackEmbedding> {
    const params = new HttpParams().set('force', force.toString());
    return this.http.post<FeedbackEmbedding>(`${this.baseUrl}/feedback/${feedbackId}`, null, { params });
  }

  generateBatchEmbeddings(batchId: string, limit: number = 50, force: boolean = false): Observable<BatchEmbeddingSummary> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('force', force.toString());
    return this.http.post<BatchEmbeddingSummary>(`${this.baseUrl}/batches/${batchId}`, null, { params });
  }

  getFeedbackEmbedding(feedbackId: string): Observable<FeedbackEmbedding> {
    return this.http.get<FeedbackEmbedding>(`${this.baseUrl}/feedback/${feedbackId}`);
  }
}
