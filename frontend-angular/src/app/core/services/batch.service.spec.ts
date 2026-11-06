import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { BatchService } from './batch.service';
import { environment } from '../../../environments/environment';

describe('BatchService', () => {
  let service: BatchService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BatchService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(BatchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should validate CSV file', () => {
    const file = new File(['header1,header2'], 'test.csv', { type: 'text/csv' });
    const mockResponse = {
      valid: true,
      rowCount: 1,
      validRows: 1,
      invalidRows: 0,
      headerValidation: true,
      sampleErrors: []
    };

    service.validateCsv(file).subscribe(res => {
      expect(res.valid).toBe(true);
      expect(res.rowCount).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/batches/validate`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should fetch batch list with pagination', () => {
    const mockResponse = {
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true
    };

    service.getBatches(0, 10).subscribe(res => {
      expect(res.content).toEqual([]);
      expect(res.totalElements).toBe(0);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/batches?page=0&size=10`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });
});
