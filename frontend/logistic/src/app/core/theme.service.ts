import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'auto' | 'light' | 'dark';

const STORAGE_KEY = 'logistic.theme';
const DARK_CLASS = 'app-dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  /** Выбор пользователя: auto следует за ОС. */
  readonly preference = signal<ThemePreference>('auto');

  /** Фактическая светлая/тёмная тема PrimeNG. */
  readonly effective = signal<'light' | 'dark'>('light');

  private mediaQuery: MediaQueryList | null = null;
  private mediaListener?: (e: MediaQueryListEvent) => void;

  constructor() {
    this.hydrateFromStorage();
    this.apply();
    this.attachMediaListener();
  }

  /** auto → light → dark → auto */
  cycle(): void {
    const order: ThemePreference[] = ['auto', 'light', 'dark'];
    const cur = this.preference();
    const i = order.indexOf(cur);
    const next = order[(i + 1) % order.length];
    this.preference.set(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* ignore */
    }
    this.apply();
    this.attachMediaListener();
  }

  /** Иконка PrimeIcons для кнопки в шапке. */
  cycleIcon(): string {
    switch (this.preference()) {
      case 'light':
        return 'pi pi-sun';
      case 'dark':
        return 'pi pi-moon';
      default:
        return 'pi pi-desktop';
    }
  }

  private hydrateFromStorage(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw === 'light' || raw === 'dark' || raw === 'auto') {
        this.preference.set(raw);
      }
    } catch {
      /* ignore */
    }
  }

  private attachMediaListener(): void {
    if (this.mediaQuery && this.mediaListener) {
      this.mediaQuery.removeEventListener('change', this.mediaListener);
      this.mediaQuery = null;
      this.mediaListener = undefined;
    }
    if (this.preference() !== 'auto') {
      return;
    }
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    this.mediaListener = () => this.apply();
    this.mediaQuery.addEventListener('change', this.mediaListener);
  }

  private apply(): void {
    const pref = this.preference();
    let dark = false;
    if (pref === 'dark') {
      dark = true;
    } else if (pref === 'light') {
      dark = false;
    } else if (
      typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function'
    ) {
      dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    } else {
      dark = false;
    }
    this.effective.set(dark ? 'dark' : 'light');
    const el = this.doc.documentElement;
    el.classList.toggle(DARK_CLASS, dark);
  }
}
