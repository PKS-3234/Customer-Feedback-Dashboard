import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { BatchService } from '../../../core/services/batch.service';
import { ValidateCsvResponse } from '../../../core/models/validation.model';
import { BatchDetail } from '../../../core/models/batch.model';

@Component({
  selector: 'app-csv-upload',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './csv-upload.component.html',
  styleUrl: './csv-upload.component.scss'
})
export class CsvUploadComponent {
  private readonly batchService = inject(BatchService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  selectedFile: File | null = null;
  isDragOver = false;
  isValidating = false;
  isUploading = false;

  validationResult: ValidateCsvResponse | null = null;
  uploadResult: BatchDetail | null = null;
  errorMessage: string | null = null;
  toastMessage: string | null = null;

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.handleFileSelected(event.dataTransfer.files[0]);
    }
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelected(input.files[0]);
    }
  }

  handleFileSelected(file: File): void {
    this.errorMessage = null;
    this.toastMessage = null;
    this.uploadResult = null;
    this.validationResult = null;

    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.errorMessage = 'Invalid file type. Please select a .csv file.';
      this.selectedFile = null;
      this.cdr.markForCheck();
      return;
    }

    this.selectedFile = file;
    this.cdr.markForCheck();
    this.validateSelectedFile();
  }

  validateSelectedFile(): void {
    if (!this.selectedFile) return;

    this.isValidating = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.batchService.validateCsv(this.selectedFile)
      .pipe(
        finalize(() => {
          this.isValidating = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.validationResult = res;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'File validation failed. Please check the file structure.';
          this.cdr.markForCheck();
        }
      });
  }

  confirmAndUpload(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.batchService.uploadBatch(this.selectedFile, 'WEB_UPLOAD', 'operator')
      .pipe(
        finalize(() => {
          this.isUploading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (batch) => {
          this.uploadResult = batch;
          this.toastMessage = `Batch ${batch.batchId.substring(0, 8)}... successfully uploaded and processed!`;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Upload processing failed.';
          this.cdr.markForCheck();
        }
      });
  }

  resetForm(): void {
    this.selectedFile = null;
    this.validationResult = null;
    this.uploadResult = null;
    this.errorMessage = null;
    this.toastMessage = null;
    this.cdr.markForCheck();
  }
}
