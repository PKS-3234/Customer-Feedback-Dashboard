import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CopilotComponent } from './copilot.component';
import { CopilotService } from '../../core/services/copilot.service';

describe('CopilotComponent', () => {
  let copilotServiceMock: Partial<CopilotService>;

  beforeEach(async () => {
    copilotServiceMock = {
      askCopilot: (query: string, limit: number) => of({
        answer: 'Customers report negative experiences with wait times.',
        agentsUsed: ['Analytics Agent', 'Reporting Agent'],
        supportingEvidence: [
          {
            agent: 'Analytics Agent',
            type: 'ANALYTICS_METRICS',
            summary: '5 records analyzed',
            data: {}
          }
        ],
        executionPath: ['routing_node', 'analytics_node', 'reporting_node']
      })
    };

    await TestBed.configureTestingModule({
      imports: [CopilotComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CopilotService, useValue: copilotServiceMock }
      ]
    }).compileComponents();
  });

  it('should create CopilotComponent', () => {
    const fixture = TestBed.createComponent(CopilotComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should execute query and populate response with execution path', () => {
    const fixture = TestBed.createComponent(CopilotComponent);
    const component = fixture.componentInstance;
    component.query = 'What are customers unhappy about?';

    component.ask();

    expect(component.isLoading).toBe(false);
    expect(component.hasQueried).toBe(true);
    expect(component.response).toBeTruthy();
    expect(component.response?.answer).toContain('wait times');
    expect(component.response?.agentsUsed).toContain('Analytics Agent');
    expect(component.response?.executionPath).toEqual(['routing_node', 'analytics_node', 'reporting_node']);
  });

  it('should handle example question selection', () => {
    const fixture = TestBed.createComponent(CopilotComponent);
    const component = fixture.componentInstance;

    component.selectExample('Show duplicate charge complaints.');

    expect(component.query).toBe('Show duplicate charge complaints.');
    expect(component.response).toBeTruthy();
  });

  it('should toggle raw evidence view', () => {
    const fixture = TestBed.createComponent(CopilotComponent);
    const component = fixture.componentInstance;

    expect(component.showRawEvidence).toBe(false);
    component.toggleRawEvidence();
    expect(component.showRawEvidence).toBe(true);
  });

  it('should reset isLoading to false, set hasQueried to true, and display errorMessage on error', () => {
    const fixture = TestBed.createComponent(CopilotComponent);
    const component = fixture.componentInstance;
    const copilotService = TestBed.inject(CopilotService);
    vi.spyOn(copilotService, 'askCopilot').mockReturnValue(
      throwError(() => ({ error: { message: 'Multi-agent orchestration timed out' } }))
    );

    component.query = 'What are top complaints?';
    component.ask();
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    expect(component.hasQueried).toBe(true);
    expect(component.errorMessage).toBe('Multi-agent orchestration timed out');
  });
});
