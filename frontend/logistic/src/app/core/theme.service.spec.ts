import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ThemeService, ThemePreference } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let doc: Document;
  const STORAGE_KEY = 'logistic.theme';

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [ThemeService],
    });
    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.classList.remove('app-dark');
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.clear();
    doc.documentElement.classList.remove('app-dark');
  });

  it('persists preference to localStorage on cycle', () => {
    const start = service.preference();
    service.cycle();
    const after = service.preference();
    expect(after).not.toBe(start);
    const stored = localStorage.getItem(STORAGE_KEY) as ThemePreference;
    expect(stored).toBe(after);
  });

  it('applies app-dark after cycling to dark (auto→light→dark)', () => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const s = TestBed.inject(ThemeService);
    const d = TestBed.inject(DOCUMENT);
    s.cycle();
    s.cycle();
    expect(s.preference()).toBe('dark');
    expect(d.documentElement.classList.contains('app-dark')).toBe(true);
  });

  it('hydrates from localStorage on first inject', () => {
    localStorage.setItem(STORAGE_KEY, 'dark');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const s2 = TestBed.inject(ThemeService);
    expect(s2.preference()).toBe('dark');
  });
});
