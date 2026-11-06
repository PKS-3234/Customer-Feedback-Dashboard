import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CsvUploadComponent } from './pages/data-operations/upload/csv-upload.component';
import { BatchListComponent } from './pages/data-operations/batches/batch-list.component';
import { BatchDetailComponent } from './pages/data-operations/batches/batch-detail.component';
import { AnalysisComponent } from './pages/analysis/analysis.component';
import { SemanticSearchComponent } from './pages/semantic-search/semantic-search.component';
import { CopilotComponent } from './pages/copilot/copilot.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'data-operations/upload', component: CsvUploadComponent },
  { path: 'data-operations/batches', component: BatchListComponent },
  { path: 'data-operations/batches/:id', component: BatchDetailComponent },
  { path: 'analysis', component: AnalysisComponent },
  { path: 'semantic-search', component: SemanticSearchComponent },
  { path: 'copilot', component: CopilotComponent },
  { path: '**', redirectTo: 'dashboard' }
];
