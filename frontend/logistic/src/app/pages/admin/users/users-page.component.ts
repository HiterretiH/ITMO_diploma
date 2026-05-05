import { Component } from '@angular/core';

/** Минимальная страница; создание пользователя из меню — фаза E8. */
@Component({
  selector: 'app-users-page',
  standalone: true,
  template: `
    <h1 class="page-title">Пользователи</h1>
    <p class="hint">Создание пользователя — пункт «+ Пользователь» в верхнем меню.</p>
  `,
  styles: [
    `
      .page-title {
        margin: 0 0 0.5rem;
        font-size: 1.25rem;
      }
      .hint {
        color: var(--p-text-muted-color, #666);
        margin: 0;
      }
    `,
  ],
})
export class UsersPageComponent {}
