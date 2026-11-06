import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { HomeComponent } from './home.component';
import { HealthService } from '../../core/services/health.service';

describe('HomeComponent', () => {
  let healthServiceMock: Partial<HealthService>;

  beforeEach(async () => {
    healthServiceMock = {
      checkBackendHealth: () => of({
        status: 'UP',
        service: 'customer-feedback-platform-backend',
        version: '1.0.0',
        timestamp: '2026-11-05T06:00:00Z'
      })
    };

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: HealthService, useValue: healthServiceMock }
      ]
    }).compileComponents();
  });

  it('should create HomeComponent', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render title heading', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Customer Feedback Intelligence Platform');
  });

  it('should display the configured Spring Boot API URL', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('http://localhost:8080/api/v1');
  });

  it('should display backend UP status when service responds', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Backend Status: UP');
  });
});
