import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, Card, Button],
  template: `
    <div class="wrap">
      <p-card header="Страница не найдена">
        <p>Запрошенный адрес не существует (404).</p>
        <p-button label="К рейсам" routerLink="/orders" />
      </p-card>
    </div>
  `,
  styles: [
    `
      .wrap {
        max-width: 28rem;
        margin: 3rem auto;
        padding: 0 1rem;
      }
    `,
  ],
})
export class NotFoundComponent {}
