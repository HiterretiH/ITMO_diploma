import { Component } from '@angular/core';

/** Заглушка маршрута; CRUD в фазе E4. */
@Component({
  selector: 'app-counterparties-page',
  standalone: true,
  template: `<h1 class="page-title">Контрагенты</h1>`,
  styles: [
    `
      .page-title {
        margin: 0 0 1rem;
        font-size: 1.25rem;
      }
    `,
  ],
})
export class CounterpartiesPageComponent {}
