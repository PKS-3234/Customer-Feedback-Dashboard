import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BatchAnalysisSummary, FeedbackAnalysis } from '../models/analysis.model';

@Injectable({
  providedIn: 'root'
})
export class AnalysisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/analysis`;

  analyzeFeedback(feedbackId: string, reanalyze = false): Observable<FeedbackAnalysis> {
    const params = new HttpParams().set('reanalyze', reanalyze.toString());
    return this.http.post<FeedbackAnalysis>(`${this.baseUrl}/feedback/${feedbackId}`, {}, { params });
  }

  analyzeBatch(batchId: string, limit = 10, reanalyze = false): Observable<BatchAnalysisSummary> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('reanalyze', reanalyze.toString());
    return this.http.post<BatchAnalysisSummary>(`${this.baseUrl}/batches/${batchId}`, {}, { params });
  }

  getCurrentAnalysis(feedbackId: string): Observable<FeedbackAnalysis> {
    return this.http.get<FeedbackAnalysis>(`${this.baseUrl}/feedback/${feedbackId}`);
  }

  getBatchAnalyses(batchId: string): Observable<FeedbackAnalysis[]> {
    return this.http.get<FeedbackAnalysis[]>(`${this.baseUrl}/batches/${batchId}`);
  }

  getAnalysisHistory(feedbackId: string): Observable<FeedbackAnalysis[]> {
    return this.http.get<FeedbackAnalysis[]>(`${this.baseUrl}/feedback/${feedbackId}/history`);
  }
}
