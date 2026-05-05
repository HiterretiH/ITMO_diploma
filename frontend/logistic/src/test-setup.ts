import '@angular/compiler';
import '@analogjs/vitest-angular/setup-zone';
import { getTestBed } from '@angular/core/testing';

/** jsdom has no ResizeObserver; PrimeNG TabList uses it in tests. */
globalThis.ResizeObserver = class ResizeObserver {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
};
import {
  BrowserTestingModule,
  platformBrowserTesting,
} from '@angular/platform-browser/testing';

getTestBed().initTestEnvironment(
  BrowserTestingModule,
  platformBrowserTesting(),
);
