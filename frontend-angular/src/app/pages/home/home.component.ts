import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { HealthService } from '../../core/services/health.service';
import { HealthResponse } from '../../core/models/health.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private readonly healthService = inject(HealthService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly title = 'Customer Feedback Intelligence Platform';
  readonly subtitle = 'Enterprise-grade customer feedback ingestion, AI analysis, embeddings & multi-agent workflows';
  readonly apiBaseUrl = environment.apiBaseUrl;
  readonly aiServiceBaseUrl = environment.aiServiceBaseUrl;
  readonly isProduction = environment.production;

  backendHealth: HealthResponse | null = null;
  loadingHealth = true;

  readonly architectureLayers = [
    { name: 'Frontend', tech: 'Angular 21 + TypeScript + SCSS', port: '4200', role: 'SPA Client with Standalone Components' },
    { name: 'Business Backend', tech: 'Java 21 + Spring Boot 3.3+ + Maven', port: '8080', role: 'Enterprise Business Logic & REST Gateway' },
    { name: 'AI Service', tech: 'Python 3.12 + FastAPI', port: '8000', role: 'AI Analysis, Embeddings & Multi-Agent Workflows' },
    { name: 'Database & Vector Store', tech: 'PostgreSQL 16 + PGVector', port: '5432', role: 'Relational & High-Dimensional Vector Storage' }
  ];

  readonly roadmapPhases = [
    { id: 1, name: 'Infrastructure Foundation', status: 'Completed (Verified)' },
    { id: 2, name: 'CSV Ingestion Platform', status: 'Completed (Phase 2)' },
    { id: 3, name: 'Gemini AI Analysis', status: 'Scheduled (Phase 3)' },
    { id: 4, name: 'Embeddings & Semantic Search', status: 'Scheduled (Phase 4)' },
    { id: 5, name: 'Multi-Agent System', status: 'Scheduled (Phase 5)' },
    { id: 6, name: 'Executive Dashboard', status: 'Scheduled (Phase 6)' },
    { id: 7, name: 'LangGraph Orchestration', status: 'Scheduled (Phase 7)' }
  ];

  ngOnInit(): void {
    this.refreshHealth();
  }

  refreshHealth(): void {
    this.loadingHealth = true;
    this.cdr.markForCheck();
    this.healthService.checkBackendHealth()
      .pipe(
        finalize(() => {
          this.loadingHealth = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.backendHealth = res;
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        }
      });
  }
}
