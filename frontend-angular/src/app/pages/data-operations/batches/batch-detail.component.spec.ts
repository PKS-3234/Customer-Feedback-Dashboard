import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BatchDetailComponent } from './batch-detail.component';
import { BatchService } from '../../../core/services/batch.service';

describe('BatchDetailComponent', () => {
  let batchServiceMock: Partial<BatchService>;

  beforeEach(async () => {
    batchServiceMock = {
      getBatchDetail: () => of({
        batchId: '123e4567-e89b-12d3-a456-426614174000',
        sourceFileName: 'feedback.csv',
        sourceSystem: 'WEB_UPLOAD',
        uploadUser: 'operator',
        fileHash: '1234567890abcdef',
        totalRows: 10,
        successfulRows: 9,
        failedRows: 1,
        duplicateRows: 0,
        status: 'COMPLETED_WITH_ERRORS',
        createdAt: '2026-11-05T06:00:00Z'
      }),
      getBatchErrors: () => of({
        content: [
          {
            errorId: 'err-1',
            batchId: '123e4567-e89b-12d3-a456-426614174000',
            rowNumber: 3,
            rawRecord: 'bad,record',
            errorType: 'RATING_INVALID',
            errorMessage: 'Rating out of range',
            createdAt: '2026-11-05T06:00:00Z'
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        last: true
      }),
      getFeedback: () => of({
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        last: true
      })
    };

    await TestBed.configureTestingModule({
      imports: [BatchDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '123e4567-e89b-12d3-a456-426614174000'
              }
            }
          }
        },
        { provide: BatchService, useValue: batchServiceMock }
      ]
    }).compileComponents();
  });

  it('should create BatchDetailComponent', () => {
    const fixture = TestBed.createComponent(BatchDetailComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render batch summary and metrics', () => {
    const fixture = TestBed.createComponent(BatchDetailComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('feedback.csv');
    expect(compiled.textContent).toContain('123e4567');
    expect(compiled.textContent).toContain('COMPLETED_WITH_ERRORS');
  });

  it('should reset isLoading to false and display errorMessage when batch fails to load', () => {
    const fixture = TestBed.createComponent(BatchDetailComponent);
    const component = fixture.componentInstance;
    const batchService = TestBed.inject(BatchService);
    vi.spyOn(batchService, 'getBatchDetail').mockReturnValue(
      throwError(() => ({ error: { message: 'Batch not found' } }))
    );

    component.loadBatchDetails();
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Batch not found');
  });
});
