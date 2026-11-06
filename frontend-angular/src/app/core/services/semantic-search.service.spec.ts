import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SemanticSearchService } from './semantic-search.service';
import { environment } from '../../../environments/environment';

describe('SemanticSearchService', () => {
  let service: SemanticSearchService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SemanticSearchService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SemanticSearchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should post search request and return ranked results', () => {
    const mockResults = [
      {
        feedbackId: 'fb-1',
        feedbackText: 'Duplicate payment on statement',
        similarity: 0.92,
        category: 'Cards',
        theme: 'Duplicate Charge',
        sentiment: 'NEGATIVE' as const,
        priority: 'HIGH' as const
      }
    ];

    service.search('duplicate payment', 5).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].similarity).toBe(0.92);
      expect(res[0].category).toBe('Cards');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/semantic-search`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ query: 'duplicate payment', limit: 5 });
    req.flush(mockResults);
  });
});
