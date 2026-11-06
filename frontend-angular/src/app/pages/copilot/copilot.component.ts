import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { CopilotService } from '../../core/services/copilot.service';
import { AgentQueryResponse } from '../../core/models/copilot.model';

@Component({
  selector: 'app-copilot',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './copilot.component.html',
  styleUrl: './copilot.component.scss'
})
export class CopilotComponent implements OnInit {
  private readonly copilotService = inject(CopilotService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  query = '';
  limit = 5;

  exampleQuestions: string[] = [
    'What are customers unhappy about?',
    'What are top themes?',
    'Show duplicate charge complaints.',
    'What is embedding coverage?'
  ];

  response: AgentQueryResponse | null = null;
  isLoading = false;
  hasQueried = false;
  errorMessage: string | null = null;
  lastQuery = '';
  showRawEvidence = false;

  ngOnInit(): void {
    const qParam = this.route.snapshot.queryParamMap.get('q');
    if (qParam && qParam.trim()) {
      this.query = qParam.trim();
      this.ask();
    }
  }

  ask(): void {
    const trimmed = this.query.trim();
    if (!trimmed) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.lastQuery = trimmed;
    this.cdr.markForCheck();

    this.copilotService.askCopilot(trimmed, this.limit)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.hasQueried = true;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (res) => {
          this.response = res;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || err.message || 'Failed to query multi-agent copilot.';
          this.cdr.markForCheck();
        }
      });
  }

  selectExample(q: string): void {
    this.query = q;
    this.ask();
  }

  clear(): void {
    this.query = '';
    this.cdr.markForCheck();
  }

  toggleRawEvidence(): void {
    this.showRawEvidence = !this.showRawEvidence;
    this.cdr.markForCheck();
  }
}
