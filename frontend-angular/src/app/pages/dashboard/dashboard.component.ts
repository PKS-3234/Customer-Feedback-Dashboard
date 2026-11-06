import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardResponse, AgentStatus } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  dashboardData: DashboardResponse | null = null;
  agentsList: AgentStatus[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.isLoading = true;
    this.errorMessage = null;

    forkJoin({
      dashboard: this.dashboardService.getDashboard(),
      agents: this.dashboardService.getAgents().pipe(
        catchError((err) => {
          console.warn('Failed to load agents status:', err);
          return of([] as AgentStatus[]);
        })
      )
    })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (result) => {
          this.dashboardData = result.dashboard;
          this.agentsList = result.agents;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || err.message || 'Failed to load executive dashboard data.';
          this.cdr.markForCheck();
        }
      });
  }

  askInCopilot(question: string): void {
    this.router.navigate(['/copilot'], { queryParams: { q: question } });
  }

  getSentimentBadgeClass(label: string): string {
    const upper = label.toUpperCase();
    if (upper === 'POSITIVE') return 'badge-positive';
    if (upper === 'NEGATIVE') return 'badge-negative';
    return 'badge-neutral';
  }

  getPriorityBadgeClass(label: string): string {
    const upper = label.toUpperCase();
    if (upper === 'HIGH' || upper === 'CRITICAL') return 'badge-high';
    if (upper === 'MEDIUM') return 'badge-medium';
    return 'badge-low';
  }
}
