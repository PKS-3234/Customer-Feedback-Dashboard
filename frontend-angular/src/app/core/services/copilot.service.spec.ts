import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CopilotService } from './copilot.service';
import { environment } from '../../../environments/environment';

describe('CopilotService', () => {
  let service: CopilotService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CopilotService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CopilotService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should post agent query and receive response', () => {
    const mockResponse = {
      answer: 'Executive summary on customer unhappiness.',
      agentsUsed: ['Analytics Agent', 'Reporting Agent'],
      supportingEvidence: [
        {
          agent: 'Analytics Agent',
          type: 'ANALYTICS_METRICS',
          summary: 'Analyzed 5 records',
          data: {}
        }
      ]
    };

    service.askCopilot('What are customers unhappy about?', 5).subscribe((res) => {
      expect(res.answer).toBe('Executive summary on customer unhappiness.');
      expect(res.agentsUsed.length).toBe(2);
      expect(res.supportingEvidence.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/agents/query`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ query: 'What are customers unhappy about?', limit: 5 });
    req.flush(mockResponse);
  });
});
