import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, Card, Button],
  template: `
    <div class="wrap">
      <p-card header="Доступ запрещён">
        <p>У вас недостаточно прав для этого раздела (403).</p>
        <p-button label="На главную" routerLink="/trips" />
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
export class ForbiddenComponent {}
