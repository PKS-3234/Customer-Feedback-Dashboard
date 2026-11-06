import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BatchListComponent } from './batch-list.component';
import { BatchService } from '../../../core/services/batch.service';

describe('BatchListComponent', () => {
  let batchServiceMock: Partial<BatchService>;

  beforeEach(async () => {
    batchServiceMock = {
      getBatches: () => of({
        content: [
          {
            batchId: '123e4567-e89b-12d3-a456-426614174000',
            sourceFileName: 'feedback_nov.csv',
            sourceSystem: 'WEB_UPLOAD',
            uploadUser: 'operator',
            totalRows: 10,
            successfulRows: 8,
            failedRows: 1,
            duplicateRows: 1,
            status: 'COMPLETED_WITH_ERRORS',
            createdAt: '2026-11-05T06:00:00Z'
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        last: true
      })
    };

    await TestBed.configureTestingModule({
      imports: [BatchListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BatchService, useValue: batchServiceMock }
      ]
    }).compileComponents();
  });

  it('should create BatchListComponent', () => {
    const fixture = TestBed.createComponent(BatchListComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should display loaded batch in table', () => {
    const fixture = TestBed.createComponent(BatchListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('feedback_nov.csv');
    expect(compiled.textContent).toContain('COMPLETED_WITH_ERRORS');
    expect(compiled.textContent).toContain('123e4567');
  });

  it('should reset isLoading to false and set errorMessage on error', () => {
    const fixture = TestBed.createComponent(BatchListComponent);
    const component = fixture.componentInstance;
    const batchService = TestBed.inject(BatchService);
    vi.spyOn(batchService, 'getBatches').mockReturnValue(
      throwError(() => ({ error: { message: 'Failed to fetch batch catalog' } }))
    );

    component.loadBatches(0);
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Failed to fetch batch catalog');
  });
});
