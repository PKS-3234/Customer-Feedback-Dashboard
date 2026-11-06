import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SemanticSearchService } from '../../core/services/semantic-search.service';
import { SemanticSearchResult } from '../../core/models/semantic-search.model';

@Component({
  selector: 'app-semantic-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './semantic-search.component.html',
  styleUrl: './semantic-search.component.scss'
})
export class SemanticSearchComponent {
  private readonly searchService = inject(SemanticSearchService);
  private readonly cdr = inject(ChangeDetectorRef);

  searchQuery = '';
  limit = 10;

  suggestedQueries: string[] = [
    'login issue',
    'duplicate payment',
    'app crash',
    'refund delay'
  ];

  results: SemanticSearchResult[] = [];
  isSearching = false;
  hasSearched = false;
  errorMessage: string | null = null;
  lastExecutedQuery = '';

  onSearch(): void {
    const trimmed = this.searchQuery.trim();
    if (!trimmed) {
      return;
    }

    this.isSearching = true;
    this.errorMessage = null;
    this.lastExecutedQuery = trimmed;
    this.cdr.markForCheck();

    this.searchService.search(trimmed, this.limit)
      .pipe(
        finalize(() => {
          this.isSearching = false;
          this.hasSearched = true;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.results = data;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || err.message || 'Semantic search failed. Please try again.';
          this.cdr.markForCheck();
        }
      });
  }

  selectSuggestion(query: string): void {
    this.searchQuery = query;
    this.onSearch();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.results = [];
    this.hasSearched = false;
    this.errorMessage = null;
    this.lastExecutedQuery = '';
    this.cdr.markForCheck();
  }

  getSimilarityPercentage(similarity: number): string {
    return (similarity * 100).toFixed(1) + '%';
  }

  getSentimentClass(sentiment: string): string {
    switch (sentiment) {
      case 'POSITIVE': return 'badge-sentiment-pos';
      case 'NEGATIVE': return 'badge-sentiment-neg';
      default: return 'badge-sentiment-neu';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'CRITICAL': return 'badge-priority-crit';
      case 'HIGH': return 'badge-priority-high';
      case 'MEDIUM': return 'badge-priority-med';
      default: return 'badge-priority-low';
    }
  }
}
