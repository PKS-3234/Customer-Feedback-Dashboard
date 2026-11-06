import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { BatchService } from '../../../core/services/batch.service';
import { BatchDetail } from '../../../core/models/batch.model';
import { ProcessingError } from '../../../core/models/error.model';
import { FeedbackItem } from '../../../core/models/feedback.model';

@Component({
  selector: 'app-batch-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './batch-detail.component.html',
  styleUrl: './batch-detail.component.scss'
})
export class BatchDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly batchService = inject(BatchService);
  private readonly cdr = inject(ChangeDetectorRef);

  batchId = '';
  batch: BatchDetail | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  // Errors tab/pagination
  errors: ProcessingError[] = [];
  errorsPage = 0;
  errorsTotalPages = 0;
  errorsTotalElements = 0;
  loadingErrors = false;

  // Stored Feedback tab/pagination
  feedbackList: FeedbackItem[] = [];
  feedbackPage = 0;
  feedbackTotalPages = 0;
  feedbackTotalElements = 0;
  loadingFeedback = false;

  activeTab: 'errors' | 'feedback' = 'errors';

  ngOnInit(): void {
    this.batchId = this.route.snapshot.paramMap.get('id') || '';
    if (this.batchId) {
      this.loadBatchDetails();
      this.loadErrors();
      this.loadFeedback();
    } else {
      this.errorMessage = 'No Batch ID specified in URL.';
      this.isLoading = false;
      this.cdr.markForCheck();
    }
  }

  loadBatchDetails(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.batchService.getBatchDetail(this.batchId)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.batch = data;
          if (data.failedRows === 0 && data.duplicateRows === 0) {
            this.activeTab = 'feedback';
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to retrieve batch details.';
          this.cdr.markForCheck();
        }
      });
  }

  loadErrors(page = 0): void {
    this.loadingErrors = true;
    this.errorsPage = page;
    this.cdr.markForCheck();

    this.batchService.getBatchErrors(this.batchId, this.errorsPage, 10)
      .pipe(
        finalize(() => {
          this.loadingErrors = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.errors = res.content;
          this.errorsTotalPages = res.totalPages;
          this.errorsTotalElements = res.totalElements;
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        }
      });
  }

  loadFeedback(page = 0): void {
    this.loadingFeedback = true;
    this.feedbackPage = page;
    this.cdr.markForCheck();

    this.batchService.getFeedback(this.batchId, this.feedbackPage, 10)
      .pipe(
        finalize(() => {
          this.loadingFeedback = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.feedbackList = res.content;
          this.feedbackTotalPages = res.totalPages;
          this.feedbackTotalElements = res.totalElements;
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        }
      });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge-success';
      case 'COMPLETED_WITH_ERRORS':
        return 'badge-warn';
      case 'PROCESSING':
        return 'badge-info';
      case 'FAILED':
      default:
        return 'badge-error';
    }
  }
}
