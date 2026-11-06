import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AnalysisComponent } from './analysis.component';
import { AnalysisService } from '../../core/services/analysis.service';
import { EmbeddingService } from '../../core/services/embedding.service';
import { BatchService } from '../../core/services/batch.service';

describe('AnalysisComponent', () => {
  let analysisServiceMock: Partial<AnalysisService>;
  let embeddingServiceMock: Partial<EmbeddingService>;
  let batchServiceMock: Partial<BatchService>;

  beforeEach(async () => {
    embeddingServiceMock = {
      generateBatchEmbeddings: () => of({
        batchId: '123e4567-e89b-12d3-a456-426614174000',
        totalRequested: 10,
        generatedCount: 5,
        skippedCount: 0,
        failedCount: 0,
        results: []
      })
    };
    batchServiceMock = {
      getBatches: () => of({
        content: [
          {
            batchId: '123e4567-e89b-12d3-a456-426614174000',
            sourceFileName: 'test_batch.csv',
            sourceSystem: 'WEB_UPLOAD',
            uploadUser: 'operator',
            totalRows: 5,
            successfulRows: 5,
            failedRows: 0,
            duplicateRows: 0,
            status: 'COMPLETED',
            createdAt: '2026-11-05T06:00:00Z'
          }
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true
      })
    };

    analysisServiceMock = {
      getBatchAnalyses: () => of([
        {
          analysisId: 'a1',
          feedbackId: 'fb1',
          sentiment: 'POSITIVE',
          category: 'Mobile App',
          theme: 'Biometric Login',
          emotion: 'Happy',
          priority: 'LOW',
          department: 'Digital Banking',
          shortSummary: 'Customer loves the fast biometric sign-in.',
          confidenceScore: 0.95,
          modelName: 'gemini-2.5-flash',
          modelVersion: 'gemini-2.5-flash',
          isCurrent: true,
          createdAt: '2026-11-05T07:00:00Z'
        }
      ]),
      analyzeBatch: () => of({
        batchId: '123e4567-e89b-12d3-a456-426614174000',
        totalRequested: 10,
        analyzedCount: 1,
        skippedCount: 0,
        failedCount: 0,
        results: []
      })
    };

    await TestBed.configureTestingModule({
      imports: [AnalysisComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BatchService, useValue: batchServiceMock },
        { provide: AnalysisService, useValue: analysisServiceMock },
        { provide: EmbeddingService, useValue: embeddingServiceMock }
      ]
    }).compileComponents();
  });

  it('should create AnalysisComponent', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render analysis controls and loaded batch', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('h1')?.textContent).toContain('Gemini Feedback Analysis');
    expect(compiled.textContent).toContain('test_batch.csv');
  });

  it('should display analysis results in grid', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Biometric Login');
    expect(compiled.textContent).toContain('POSITIVE');
    expect(compiled.textContent).toContain('Digital Banking');
    expect(compiled.textContent).toContain('95%');
  });

  it('should reset isAnalyzing to false and set errorMessage on batch analysis failure', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    const component = fixture.componentInstance;
    const analysisService = TestBed.inject(AnalysisService);
    vi.spyOn(analysisService, 'analyzeBatch').mockReturnValue(
      throwError(() => ({ error: { message: 'Gemini service quota exceeded' } }))
    );

    component.batchId = '123e4567-e89b-12d3-a456-426614174000';
    component.runBatchAnalysis();
    fixture.detectChanges();

    expect(component.isAnalyzing).toBe(false);
    expect(component.errorMessage).toBe('Gemini service quota exceeded');
  });

  it('should execute generateBatchEmbeddings and set lastEmbeddingSummary', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    const component = fixture.componentInstance;
    component.batchId = '123e4567-e89b-12d3-a456-426614174000';

    component.generateBatchEmbeddings();
    fixture.detectChanges();

    expect(component.isGeneratingEmbeddings).toBe(false);
    expect(component.lastEmbeddingSummary).toBeTruthy();
    expect(component.lastEmbeddingSummary?.generatedCount).toBe(5);
    expect(component.successMessage).toContain('Vector embeddings complete');
  });

  it('should reset isGeneratingEmbeddings to false and set errorMessage on embedding generation failure', () => {
    const fixture = TestBed.createComponent(AnalysisComponent);
    const component = fixture.componentInstance;
    const embeddingService = TestBed.inject(EmbeddingService);
    vi.spyOn(embeddingService, 'generateBatchEmbeddings').mockReturnValue(
      throwError(() => ({ error: { message: 'AI embedding service unavailable' } }))
    );

    component.batchId = '123e4567-e89b-12d3-a456-426614174000';
    component.generateBatchEmbeddings();
    fixture.detectChanges();

    expect(component.isGeneratingEmbeddings).toBe(false);
    expect(component.errorMessage).toBe('AI embedding service unavailable');
  });
});
