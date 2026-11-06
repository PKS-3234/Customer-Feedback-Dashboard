import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SemanticSearchRequest, SemanticSearchResult } from '../models/semantic-search.model';

@Injectable({
  providedIn: 'root'
})
export class SemanticSearchService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/semantic-search`;

  search(query: string, limit: number = 10): Observable<SemanticSearchResult[]> {
    const payload: SemanticSearchRequest = { query: query.trim(), limit };
    return this.http.post<SemanticSearchResult[]>(this.apiUrl, payload);
  }
}
