import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { interval, startWith, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { RegistrationStatus } from '../../core/auth.models';
import { GuestHeaderComponent } from '../../shared/guest-header/guest-header.component';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Message } from 'primeng/message';

const CONTACT_EMAIL = 'contact@truck-docs.ru';
const POLL_INTERVAL_MS = 10_000;

@Component({
  selector: 'app-registration-status',
  standalone: true,
  imports: [CommonModule, GuestHeaderComponent, RouterLink, Card, Button, Message],
  templateUrl: './registration-status.component.html',
  styleUrl: './registration-status.component.css',
})
export class RegistrationStatusComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly contactEmail = CONTACT_EMAIL;
  username: string | null = null;
  status: RegistrationStatus | null = null;
  loadError: string | null = null;

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      void this.router.navigateByUrl('/orders');
      return;
    }

    this.username = this.auth.getPendingRegistrationUsername();
    if (!this.username) {
      void this.router.navigateByUrl('/register');
      return;
    }

    interval(POLL_INTERVAL_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.auth.getRegistrationStatus(this.username!)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.loadError = null;
          this.status = response.registrationStatus;
          if (response.registrationStatus === 'APPROVED') {
            this.auth.clearPendingRegistrationUsername();
            void this.router.navigate(['/login'], {
              queryParams: { approved: '1' },
            });
          }
        },
        error: () => {
          this.loadError = 'Не удалось получить статус регистрации';
        },
      });
  }

  statusLabel(): string {
    switch (this.status) {
      case 'PENDING':
        return 'Ожидание подтверждения';
      case 'REJECTED':
        return 'Отклонено';
      default:
        return 'Загрузка…';
    }
  }
}
