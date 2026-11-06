import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AnalysisService } from './analysis.service';
import { environment } from '../../../environments/environment';

describe('AnalysisService', () => {
  let service: AnalysisService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AnalysisService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AnalysisService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should analyze single feedback record', () => {
    const feedbackId = '11111111-1111-1111-1111-111111111111';
    const mockAnalysis = {
      analysisId: 'a1',
      feedbackId,
      sentiment: 'POSITIVE' as const,
      category: 'Mobile App',
      theme: 'Biometrics',
      emotion: 'Happy' as const,
      priority: 'LOW' as const,
      department: 'Digital Banking',
      shortSummary: 'Great login speed.',
      confidenceScore: 0.95,
      modelName: 'gemini-2.5-flash',
      modelVersion: 'gemini-2.5-flash',
      isCurrent: true,
      createdAt: '2026-11-05T07:00:00Z'
    };

    service.analyzeFeedback(feedbackId, false).subscribe(res => {
      expect(res.sentiment).toBe('POSITIVE');
      expect(res.category).toBe('Mobile App');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/analysis/feedback/${feedbackId}?reanalyze=false`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAnalysis);
  });

  it('should analyze batch of records', () => {
    const batchId = '22222222-2222-2222-2222-222222222222';
    const mockSummary = {
      batchId,
      totalRequested: 10,
      analyzedCount: 5,
      skippedCount: 5,
      failedCount: 0,
      results: []
    };

    service.analyzeBatch(batchId, 10, false).subscribe(res => {
      expect(res.analyzedCount).toBe(5);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/analysis/batches/${batchId}?limit=10&reanalyze=false`);
    expect(req.request.method).toBe('POST');
    req.flush(mockSummary);
  });
});
