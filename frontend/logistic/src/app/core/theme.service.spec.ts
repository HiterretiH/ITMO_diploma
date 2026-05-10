import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  const STORAGE_KEY = 'logistic.theme';

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('app-dark');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('app-dark');
  });

  it('persists preference to localStorage on cycle', () => {
    localStorage.setItem(STORAGE_KEY, 'light');
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const service = TestBed.inject(ThemeService);
    expect(service.preference()).toBe('light');
    service.cycle();
    expect(service.preference()).toBe('dark');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark');
    service.cycle();
    expect(service.preference()).toBe('light');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('light');
  });

  it('applies app-dark when preference is dark', () => {
    localStorage.setItem(STORAGE_KEY, 'dark');
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const d = TestBed.inject(DOCUMENT);
    const s = TestBed.inject(ThemeService);
    expect(s.preference()).toBe('dark');
    expect(d.documentElement.classList.contains('app-dark')).toBe(true);
  });

  it('hydrates from localStorage on inject', () => {
    localStorage.setItem(STORAGE_KEY, 'dark');
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const s = TestBed.inject(ThemeService);
    expect(s.preference()).toBe('dark');
  });

  it('on first visit uses prefers-color-scheme once and stores explicit theme', () => {
    localStorage.clear();
    const matchMediaSpy = vi.fn().mockReturnValue({
      matches: true,
      media: '',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    } as unknown as MediaQueryList);
    window.matchMedia = matchMediaSpy;

    TestBed.configureTestingModule({ providers: [ThemeService] });
    const s = TestBed.inject(ThemeService);

    expect(s.preference()).toBe('dark');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark');
    expect(matchMediaSpy).toHaveBeenCalledWith('(prefers-color-scheme: dark)');
  });

  it('migrates legacy auto key to explicit light or dark', () => {
    localStorage.setItem(STORAGE_KEY, 'auto');
    window.matchMedia = vi.fn().mockReturnValue({
      matches: false,
      media: '',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    } as unknown as MediaQueryList);

    TestBed.configureTestingModule({ providers: [ThemeService] });
    const s = TestBed.inject(ThemeService);

    expect(s.preference()).toBe('light');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('light');
  });
});
