import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { environment } from '../../../environments/environment';
import { DashboardResponse, AgentStatus } from '../models/dashboard.model';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DashboardService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch dashboard metrics via GET /api/v1/dashboard', () => {
    const mockDashboard: DashboardResponse = {
      keyMetrics: {
        totalFeedbackRecords: 10,
        totalAnalyzedRecords: 8,
        totalEmbeddedRecords: 8,
        positiveFeedbackPercent: 62.5,
        negativeFeedbackPercent: 37.5,
        highPriorityFeedbackCount: 2,
        currentBatches: 2,
        processingErrors: 1,
        failedRows: 1,
        duplicateRows: 0
      },
      charts: {
        sentimentDistribution: [{ label: 'POSITIVE', count: 5, percentage: 62.5 }],
        categoryDistribution: [{ label: 'Mobile App', count: 8, percentage: 100.0 }],
        themeDistribution: [{ label: 'Login Speed', count: 8, percentage: 100.0 }],
        priorityDistribution: [{ label: 'HIGH', count: 2, percentage: 25.0 }],
        feedbackByChannel: [{ label: 'MOBILE_APP', count: 10, percentage: 100.0 }],
        feedbackByProduct: [{ label: 'Checking Account', count: 10, percentage: 100.0 }],
        errorDistribution: [{ label: 'INVALID_HEADER', count: 1, percentage: 100.0 }]
      },
      semanticSearchStatus: {
        embeddingModel: 'gemini-embedding-001',
        embeddingDimension: 768,
        totalEmbeddings: 8,
        semanticSearchStatus: 'OPERATIONAL',
        pgvectorStatus: 'ENABLED (HNSW Index Active)'
      },
      exampleQuestions: ['What are customers unhappy about?']
    };

    service.getDashboard().subscribe((data) => {
      expect(data.keyMetrics.totalFeedbackRecords).toBe(10);
      expect(data.keyMetrics.positiveFeedbackPercent).toBe(62.5);
      expect(data.semanticSearchStatus.semanticSearchStatus).toBe('OPERATIONAL');
    });

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDashboard);
  });

  it('should fetch agents list via GET /api/v1/dashboard/agents', () => {
    const mockAgents: AgentStatus[] = [
      {
        name: 'Supervisor Agent',
        purpose: 'Deterministic query routing',
        availability: 'AVAILABLE',
        executionHistory: 'Not Tracked'
      },
      {
        name: 'Reporting Agent',
        purpose: 'Evidence synthesis',
        availability: 'AVAILABLE',
        executionHistory: 'Not Tracked'
      }
    ];

    service.getAgents().subscribe((agents) => {
      expect(agents.length).toBe(2);
      expect(agents[0].name).toBe('Supervisor Agent');
      expect(agents[0].executionHistory).toBe('Not Tracked');
    });

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/dashboard/agents`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAgents);
  });
});
