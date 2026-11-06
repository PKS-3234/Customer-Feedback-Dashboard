import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardResponse, AgentStatus } from '../../core/models/dashboard.model';

describe('DashboardComponent', () => {
  let dashboardServiceMock: Partial<DashboardService>;
  let router: Router;

  const mockResponse: DashboardResponse = {
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
      sentimentDistribution: [
        { label: 'POSITIVE', count: 5, percentage: 62.5 },
        { label: 'NEGATIVE', count: 3, percentage: 37.5 }
      ],
      categoryDistribution: [
        { label: 'Mobile App', count: 4, percentage: 50.0 },
        { label: 'Account Services', count: 4, percentage: 50.0 }
      ],
      themeDistribution: [
        { label: 'Login Speed', count: 4, percentage: 50.0 }
      ],
      priorityDistribution: [
        { label: 'HIGH', count: 2, percentage: 25.0 },
        { label: 'LOW', count: 6, percentage: 75.0 }
      ],
      feedbackByChannel: [
        { label: 'MOBILE_APP', count: 6, percentage: 60.0 },
        { label: 'BRANCH', count: 4, percentage: 40.0 }
      ],
      feedbackByProduct: [
        { label: 'Checking Account', count: 7, percentage: 70.0 },
        { label: 'Mortgage', count: 3, percentage: 30.0 }
      ],
      errorDistribution: [
        { label: 'rating_INVALID', count: 1, percentage: 100.0 }
      ]
    },
    semanticSearchStatus: {
      embeddingModel: 'gemini-embedding-001',
      embeddingDimension: 768,
      totalEmbeddings: 8,
      semanticSearchStatus: 'OPERATIONAL',
      pgvectorStatus: 'ENABLED (HNSW Index Active)'
    },
    exampleQuestions: [
      'What are customers unhappy about?',
      'What are top themes?',
      'Show duplicate charge complaints.',
      'What is embedding coverage?'
    ]
  };

  const mockAgents: AgentStatus[] = [
    {
      name: 'Supervisor Agent',
      purpose: 'Deterministic query intent routing and dispatching',
      availability: 'AVAILABLE',
      executionHistory: 'Not Tracked'
    },
    {
      name: 'Analytics Agent',
      purpose: 'Aggregates sentiment and category distributions',
      availability: 'AVAILABLE',
      executionHistory: 'Not Tracked'
    },
    {
      name: 'Retrieval Agent',
      purpose: 'PGVector cosine search',
      availability: 'AVAILABLE',
      executionHistory: 'Not Tracked'
    },
    {
      name: 'Data Quality Agent',
      purpose: 'Monitors pipeline coverage',
      availability: 'AVAILABLE',
      executionHistory: 'Not Tracked'
    },
    {
      name: 'Reporting Agent',
      purpose: 'Evidence-grounded summaries',
      availability: 'AVAILABLE',
      executionHistory: 'Not Tracked'
    }
  ];

  beforeEach(async () => {
    dashboardServiceMock = {
      getDashboard: () => of(mockResponse),
      getAgents: () => of(mockAgents)
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should create DashboardComponent', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should load dashboard metrics and agents on init', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    expect(component.dashboardData).toBeTruthy();
    expect(component.dashboardData?.keyMetrics.totalFeedbackRecords).toBe(10);
    expect(component.dashboardData?.keyMetrics.positiveFeedbackPercent).toBe(62.5);
    expect(component.dashboardData?.keyMetrics.negativeFeedbackPercent).toBe(37.5);
    expect(component.dashboardData?.keyMetrics.highPriorityFeedbackCount).toBe(2);
    expect(component.dashboardData?.charts.sentimentDistribution.length).toBe(2);
    expect(component.dashboardData?.charts.feedbackByChannel.length).toBe(2);
    expect(component.dashboardData?.semanticSearchStatus.pgvectorStatus).toBe('ENABLED (HNSW Index Active)');
    expect(component.agentsList.length).toBe(5);
  });

  it('should navigate to Copilot with question parameter when example clicked', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.askInCopilot('What are top themes?');

    expect(navigateSpy).toHaveBeenCalledWith(['/copilot'], {
      queryParams: { q: 'What are top themes?' }
    });
  });

  it('should return appropriate badge classes for sentiment and priority', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;

    expect(component.getSentimentBadgeClass('POSITIVE')).toBe('badge-positive');
    expect(component.getSentimentBadgeClass('NEGATIVE')).toBe('badge-negative');
    expect(component.getSentimentBadgeClass('NEUTRAL')).toBe('badge-neutral');

    expect(component.getPriorityBadgeClass('HIGH')).toBe('badge-high');
    expect(component.getPriorityBadgeClass('MEDIUM')).toBe('badge-medium');
    expect(component.getPriorityBadgeClass('LOW')).toBe('badge-low');
  });

  it('should reset isLoading to false and set errorMessage on API failure', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    const dashboardService = TestBed.inject(DashboardService);
    vi.spyOn(dashboardService, 'getDashboard').mockReturnValue(
      throwError(() => ({ error: { message: 'Failed to connect to backend telemetry service' } }))
    );

    component.refresh();
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Failed to connect to backend telemetry service');
  });
});
