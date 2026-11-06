import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { EmbeddingService } from './embedding.service';
import { environment } from '../../../environments/environment';

describe('EmbeddingService', () => {
  let service: EmbeddingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        EmbeddingService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(EmbeddingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should request single embedding generation', () => {
    const feedbackId = '11111111-1111-1111-1111-111111111111';
    const mockEmbedding = {
      embeddingId: 'emb-1',
      feedbackId,
      analysisId: 'ana-1',
      embeddingModel: 'gemini-embedding-001',
      embeddingDimension: 768,
      isCurrent: true,
      createdAt: '2026-11-05T08:00:00Z'
    };

    service.generateFeedbackEmbedding(feedbackId, false).subscribe((res) => {
      expect(res.embeddingDimension).toBe(768);
      expect(res.embeddingModel).toBe('gemini-embedding-001');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/embeddings/feedback/${feedbackId}?force=false`);
    expect(req.request.method).toBe('POST');
    req.flush(mockEmbedding);
  });

  it('should request batch embedding generation', () => {
    const batchId = '22222222-2222-2222-2222-222222222222';
    const mockSummary = {
      batchId,
      totalRequested: 5,
      generatedCount: 5,
      skippedCount: 0,
      failedCount: 0,
      results: []
    };

    service.generateBatchEmbeddings(batchId, 25, true).subscribe((res) => {
      expect(res.totalRequested).toBe(5);
      expect(res.generatedCount).toBe(5);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/embeddings/batches/${batchId}?limit=25&force=true`);
    expect(req.request.method).toBe('POST');
    req.flush(mockSummary);
  });
});
