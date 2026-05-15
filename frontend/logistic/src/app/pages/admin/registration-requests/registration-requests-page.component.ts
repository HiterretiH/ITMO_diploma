import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { AdminApiService } from '../../../core/admin-api.service';
import { RegistrationRequestResponse } from '../../../core/auth.models';

@Component({
  selector: 'app-registration-requests-page',
  standalone: true,
  imports: [CommonModule, TableModule, Button, Tag],
  templateUrl: './registration-requests-page.component.html',
  styleUrl: './registration-requests-page.component.css',
})
export class RegistrationRequestsPageComponent implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly messages = inject(MessageService);

  readonly rows = signal<RegistrationRequestResponse[]>([]);
  readonly loading = signal(false);
  private readonly busyIds = signal<Set<number>>(new Set());

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.adminApi.listRegistrationRequests().subscribe({
      next: (items) => {
        this.rows.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  isBusy(id: number): boolean {
    return this.busyIds().has(id);
  }

  approve(row: RegistrationRequestResponse): void {
    this.setBusy(row.id, true);
    this.adminApi.approveRegistrationRequest(row.id).subscribe({
      next: () => {
        this.messages.add({
          severity: 'success',
          summary: 'Регистрация подтверждена',
          detail: row.username,
          life: 4000,
        });
        this.setBusy(row.id, false);
        this.reload();
      },
      error: () => this.setBusy(row.id, false),
    });
  }

  reject(row: RegistrationRequestResponse): void {
    this.setBusy(row.id, true);
    this.adminApi.rejectRegistrationRequest(row.id).subscribe({
      next: () => {
        this.messages.add({
          severity: 'warn',
          summary: 'Регистрация отклонена',
          detail: row.username,
          life: 4000,
        });
        this.setBusy(row.id, false);
        this.reload();
      },
      error: () => this.setBusy(row.id, false),
    });
  }

  statusLabel(status: RegistrationRequestResponse['registrationStatus']): string {
    return status === 'PENDING' ? 'Ожидание' : 'Отклонено';
  }

  statusSeverity(
    status: RegistrationRequestResponse['registrationStatus'],
  ): 'warn' | 'danger' {
    return status === 'PENDING' ? 'warn' : 'danger';
  }

  private setBusy(id: number, busy: boolean): void {
    const next = new Set(this.busyIds());
    if (busy) {
      next.add(id);
    } else {
      next.delete(id);
    }
    this.busyIds.set(next);
  }
}
