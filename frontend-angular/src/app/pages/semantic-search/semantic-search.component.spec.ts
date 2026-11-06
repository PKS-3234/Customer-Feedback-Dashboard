import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { SemanticSearchComponent } from './semantic-search.component';
import { SemanticSearchService } from '../../core/services/semantic-search.service';

describe('SemanticSearchComponent', () => {
  let searchServiceMock: Partial<SemanticSearchService>;

  beforeEach(async () => {
    searchServiceMock = {
      search: (query: string, limit: number) => of([
        {
          feedbackId: 'fb-123',
          feedbackText: 'The mobile app crashed after login.',
          similarity: 0.942,
          category: 'Mobile App',
          theme: 'App Crash',
          sentiment: 'NEGATIVE',
          priority: 'HIGH'
        }
      ])
    };

    await TestBed.configureTestingModule({
      imports: [SemanticSearchComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SemanticSearchService, useValue: searchServiceMock }
      ]
    }).compileComponents();
  });

  it('should create SemanticSearchComponent', () => {
    const fixture = TestBed.createComponent(SemanticSearchComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should execute search and populate results', () => {
    const fixture = TestBed.createComponent(SemanticSearchComponent);
    const component = fixture.componentInstance;
    component.searchQuery = 'app crash';

    component.onSearch();

    expect(component.isSearching).toBe(false);
    expect(component.hasSearched).toBe(true);
    expect(component.results.length).toBe(1);
    expect(component.results[0].category).toBe('Mobile App');
    expect(component.results[0].similarity).toBe(0.942);
  });

  it('should select suggestion and trigger search', () => {
    const fixture = TestBed.createComponent(SemanticSearchComponent);
    const component = fixture.componentInstance;

    component.selectSuggestion('login issue');

    expect(component.searchQuery).toBe('login issue');
    expect(component.hasSearched).toBe(true);
    expect(component.results.length).toBe(1);
  });

  it('should clear search and reset results', () => {
    const fixture = TestBed.createComponent(SemanticSearchComponent);
    const component = fixture.componentInstance;
    component.searchQuery = 'something';
    component.results = [
      {
        feedbackId: 'fb-1',
        feedbackText: 'text',
        similarity: 0.8,
        category: 'cat',
        theme: 'thm',
        sentiment: 'NEUTRAL',
        priority: 'LOW'
      }
    ];
    component.hasSearched = true;

    component.clearSearch();

    expect(component.searchQuery).toBe('');
    expect(component.results.length).toBe(0);
    expect(component.hasSearched).toBe(false);
  });

  it('should reset isSearching to false and set errorMessage on search error', () => {
    const fixture = TestBed.createComponent(SemanticSearchComponent);
    const component = fixture.componentInstance;
    const searchService = TestBed.inject(SemanticSearchService);
    vi.spyOn(searchService, 'search').mockReturnValue(
      throwError(() => ({ error: { message: 'Vector search service unavailable' } }))
    );

    component.searchQuery = 'test query';
    component.onSearch();
    fixture.detectChanges();

    expect(component.isSearching).toBe(false);
    expect(component.hasSearched).toBe(true);
    expect(component.errorMessage).toBe('Vector search service unavailable');
  });
});
