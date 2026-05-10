import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark';

const STORAGE_KEY = 'logistic.theme';
const DARK_CLASS = 'app-dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  /** Светлая или тёмная тема (после первого визита всегда явно в localStorage). */
  readonly preference = signal<ThemePreference>('light');

  /** Фактическая светлая/тёмная тема PrimeNG (совпадает с preference). */
  readonly effective = signal<'light' | 'dark'>('light');

  constructor() {
    this.hydrateFromStorage();
    this.apply();
  }

  /** Переключение только светлая ↔ тёмная. */
  cycle(): void {
    const next: ThemePreference = this.preference() === 'light' ? 'dark' : 'light';
    this.preference.set(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* ignore */
    }
    this.apply();
  }

  /** Иконка текущей темы для кнопки в шапке. */
  cycleIcon(): string {
    return this.preference() === 'dark' ? 'pi pi-moon' : 'pi pi-sun';
  }

  private hydrateFromStorage(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw === 'light' || raw === 'dark') {
        this.preference.set(raw);
        return;
      }
      // Нет ключа, мусор или устаревшее 'auto' — один раз учитываем ОС и фиксируем выбор.
      const initial = this.systemPrefersDarkOnce() ? 'dark' : 'light';
      this.preference.set(initial);
      localStorage.setItem(STORAGE_KEY, initial);
    } catch {
      this.preference.set('light');
    }
  }

  private systemPrefersDarkOnce(): boolean {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return false;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  private apply(): void {
    const dark = this.preference() === 'dark';
    this.effective.set(dark ? 'dark' : 'light');
    const el = this.doc.documentElement;
    el.classList.toggle(DARK_CLASS, dark);
  }
}
