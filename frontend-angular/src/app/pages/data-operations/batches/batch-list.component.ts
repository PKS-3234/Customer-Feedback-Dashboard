import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { BatchService } from '../../../core/services/batch.service';
import { BatchSummary } from '../../../core/models/batch.model';
import { PagedResponse } from '../../../core/models/pagination.model';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './batch-list.component.html',
  styleUrl: './batch-list.component.scss'
})
export class BatchListComponent implements OnInit {
  private readonly batchService = inject(BatchService);
  private readonly cdr = inject(ChangeDetectorRef);

  batches: BatchSummary[] = [];
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  isLoading = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadBatches();
  }

  loadBatches(page = 0): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.currentPage = page;
    this.cdr.markForCheck();

    this.batchService.getBatches(this.currentPage, this.pageSize)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res: PagedResponse<BatchSummary>) => {
          this.batches = res.content;
          this.totalPages = res.totalPages;
          this.totalElements = res.totalElements;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to load ingestion batches.';
          this.cdr.markForCheck();
        }
      });
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.loadBatches(this.currentPage - 1);
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadBatches(this.currentPage + 1);
    }
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
