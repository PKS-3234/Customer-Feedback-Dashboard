import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AnalysisService } from '../../core/services/analysis.service';
import { EmbeddingService } from '../../core/services/embedding.service';
import { BatchService } from '../../core/services/batch.service';
import { BatchSummary } from '../../core/models/batch.model';
import { FeedbackAnalysis, BatchAnalysisSummary } from '../../core/models/analysis.model';
import { BatchEmbeddingSummary } from '../../core/models/embedding.model';

@Component({
  selector: 'app-analysis',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './analysis.component.html',
  styleUrl: './analysis.component.scss'
})
export class AnalysisComponent implements OnInit {
  private readonly analysisService = inject(AnalysisService);
  private readonly embeddingService = inject(EmbeddingService);
  private readonly batchService = inject(BatchService);
  private readonly cdr = inject(ChangeDetectorRef);

  batchId = '';
  limit = 10;
  reanalyze = false;

  availableBatches: BatchSummary[] = [];
  analyses: FeedbackAnalysis[] = [];
  lastBatchSummary: BatchAnalysisSummary | null = null;
  lastEmbeddingSummary: BatchEmbeddingSummary | null = null;

  isLoadingBatches = false;
  isAnalyzing = false;
  isGeneratingEmbeddings = false;
  isLoadingExisting = false;

  successMessage: string | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadAvailableBatches();
  }

  loadAvailableBatches(): void {
    this.isLoadingBatches = true;
    this.cdr.markForCheck();

    this.batchService.getBatches(0, 20)
      .pipe(
        finalize(() => {
          this.isLoadingBatches = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.availableBatches = res.content;
          if (this.availableBatches.length > 0 && !this.batchId) {
            this.batchId = this.availableBatches[0].batchId;
            this.loadExistingAnalyses();
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        }
      });
  }

  onBatchChange(): void {
    if (this.batchId) {
      this.loadExistingAnalyses();
    }
  }

  loadExistingAnalyses(): void {
    if (!this.batchId) return;

    this.isLoadingExisting = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.analysisService.getBatchAnalyses(this.batchId)
      .pipe(
        finalize(() => {
          this.isLoadingExisting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (results) => {
          this.analyses = results;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to load existing batch analyses.';
          this.cdr.markForCheck();
        }
      });
  }

  runBatchAnalysis(): void {
    if (!this.batchId) {
      this.errorMessage = 'Please provide or select a Batch ID.';
      this.cdr.markForCheck();
      return;
    }

    this.isAnalyzing = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.lastBatchSummary = null;
    this.cdr.markForCheck();

    this.analysisService.analyzeBatch(this.batchId, this.limit, this.reanalyze)
      .pipe(
        finalize(() => {
          this.isAnalyzing = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (summary) => {
          this.lastBatchSummary = summary;
          this.successMessage = `Analysis complete: ${summary.analyzedCount} record(s) analyzed, ${summary.skippedCount} skipped, ${summary.failedCount} failed.`;
          this.loadExistingAnalyses();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to run Gemini batch analysis. Check API key and service health.';
          this.cdr.markForCheck();
        }
      });
  }

  generateBatchEmbeddings(): void {
    if (!this.batchId) {
      this.errorMessage = 'Please provide or select a Batch ID to generate embeddings.';
      this.cdr.markForCheck();
      return;
    }

    this.isGeneratingEmbeddings = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.lastEmbeddingSummary = null;
    this.cdr.markForCheck();

    this.embeddingService.generateBatchEmbeddings(this.batchId, this.limit, this.reanalyze)
      .pipe(
        finalize(() => {
          this.isGeneratingEmbeddings = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (summary) => {
          this.lastEmbeddingSummary = summary;
          this.successMessage = `Vector embeddings complete: ${summary.generatedCount} generated & stored in PGVector, ${summary.skippedCount} skipped, ${summary.failedCount} failed.`;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to generate vector embeddings. Check AI service connectivity.';
          this.cdr.markForCheck();
        }
      });
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
