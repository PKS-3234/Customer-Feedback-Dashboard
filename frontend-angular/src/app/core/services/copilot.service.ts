import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgentQueryRequest, AgentQueryResponse } from '../models/copilot.model';

@Injectable({
  providedIn: 'root'
})
export class CopilotService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/agents/query`;

  askCopilot(query: string, limit: number = 5): Observable<AgentQueryResponse> {
    const payload: AgentQueryRequest = { query: query.trim(), limit };
    return this.http.post<AgentQueryResponse>(this.apiUrl, payload);
  }
}
