import { Component } from '@angular/core';

@Component({
  selector: 'app-vehicles-page',
  standalone: true,
  template: `<h1 class="page-title">Транспорт</h1>`,
  styles: [
    `
      .page-title {
        margin: 0 0 1rem;
        font-size: 1.25rem;
      }
    `,
  ],
})
export class VehiclesPageComponent {}
