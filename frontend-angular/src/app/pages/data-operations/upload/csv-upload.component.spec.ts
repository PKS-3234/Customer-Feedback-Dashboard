import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CsvUploadComponent } from './csv-upload.component';
import { BatchService } from '../../../core/services/batch.service';

describe('CsvUploadComponent', () => {
  let batchServiceMock: Partial<BatchService>;

  beforeEach(async () => {
    batchServiceMock = {
      validateCsv: () => of({
        valid: true,
        rowCount: 5,
        validRows: 5,
        invalidRows: 0,
        headerValidation: true,
        sampleErrors: []
      }),
      uploadBatch: () => of({
        batchId: '11111111-1111-1111-1111-111111111111',
        sourceFileName: 'test.csv',
        sourceSystem: 'WEB_UPLOAD',
        uploadUser: 'operator',
        fileHash: 'abc123hash',
        totalRows: 5,
        successfulRows: 5,
        failedRows: 0,
        duplicateRows: 0,
        status: 'COMPLETED',
        createdAt: '2026-11-05T06:00:00Z'
      })
    };

    await TestBed.configureTestingModule({
      imports: [CsvUploadComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BatchService, useValue: batchServiceMock }
      ]
    }).compileComponents();
  });

  it('should create component', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render file dropzone area', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.dropzone')).toBeTruthy();
    expect(compiled.textContent).toContain('Drag and drop your feedback CSV file here');
  });

  it('should validate CSV file when selected', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    const component = fixture.componentInstance;
    const file = new File(['header'], 'valid.csv', { type: 'text/csv' });

    component.handleFileSelected(file);
    fixture.detectChanges();

    expect(component.selectedFile).toBe(file);
    expect(component.validationResult?.rowCount).toBe(5);
    expect(component.isValidating).toBe(false);
  });

  it('should reset isValidating to false and set errorMessage on validation error', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    const component = fixture.componentInstance;
    const batchService = TestBed.inject(BatchService);
    vi.spyOn(batchService, 'validateCsv').mockReturnValue(throwError(() => ({ error: { message: 'Invalid CSV format' } })));

    const file = new File(['bad'], 'invalid.csv', { type: 'text/csv' });
    component.handleFileSelected(file);
    fixture.detectChanges();

    expect(component.isValidating).toBe(false);
    expect(component.errorMessage).toBe('Invalid CSV format');
  });

  it('should reset isUploading to false and set toastMessage on successful upload', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    const component = fixture.componentInstance;
    const file = new File(['content'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = file;

    component.confirmAndUpload();
    fixture.detectChanges();

    expect(component.isUploading).toBe(false);
    expect(component.uploadResult).toBeTruthy();
    expect(component.toastMessage).toContain('successfully uploaded');
  });

  it('should reset isUploading to false and set errorMessage on upload error', () => {
    const fixture = TestBed.createComponent(CsvUploadComponent);
    const component = fixture.componentInstance;
    const batchService = TestBed.inject(BatchService);
    vi.spyOn(batchService, 'uploadBatch').mockReturnValue(throwError(() => ({ error: { message: 'Upload rejected by server' } })));

    const file = new File(['content'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = file;

    component.confirmAndUpload();
    fixture.detectChanges();

    expect(component.isUploading).toBe(false);
    expect(component.errorMessage).toBe('Upload rejected by server');
  });
});
