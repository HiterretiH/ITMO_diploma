import { Component } from '@angular/core';

@Component({
  selector: 'app-drivers-page',
  standalone: true,
  template: `<h1 class="page-title">Водители</h1>`,
  styles: [
    `
      .page-title {
        margin: 0 0 1rem;
        font-size: 1.25rem;
      }
    `,
  ],
})
export class DriversPageComponent {}
