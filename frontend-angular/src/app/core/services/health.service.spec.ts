import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HealthService } from './health.service';
import { environment } from '../../../environments/environment';

describe('HealthService', () => {
  let service: HealthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        HealthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(HealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch backend health', () => {
    const mockHealth = {
      status: 'UP',
      service: 'customer-feedback-platform-backend',
      version: '1.0.0',
      timestamp: '2026-11-05T06:00:00Z'
    };

    service.checkBackendHealth().subscribe(res => {
      expect(res.status).toBe('UP');
      expect(res.service).toBe('customer-feedback-platform-backend');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/health`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHealth);
  });
});
