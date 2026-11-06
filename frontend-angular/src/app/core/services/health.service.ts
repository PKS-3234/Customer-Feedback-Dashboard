import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HealthResponse } from '../models/health.model';

@Injectable({
  providedIn: 'root'
})
export class HealthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/health`;

  checkBackendHealth(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(this.apiUrl).pipe(
      catchError(error => {
        return of({
          status: 'UNAVAILABLE',
          service: 'customer-feedback-platform-backend',
          version: 'unknown',
          timestamp: new Date().toISOString(),
          details: { error: error.message || 'Service unreachable' }
        });
      })
    );
  }
}
