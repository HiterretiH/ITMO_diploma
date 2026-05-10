import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { AuditApiService } from '../../core/audit-api.service';
import { AuditEventResponse } from '../../core/audit.models';
import { localizeProblemToast } from '../../core/error-messages';
import { OrderApiService } from '../../core/order-api.service';
import {
  DocumentTypeName,
  FileFormatName,
  OrderDocumentDescriptor,
  OrderResponse,
} from '../../core/order.models';
import { ProblemDetail } from '../../models/problem.models';
import { documentTypeLabelRu } from '../../shared/document-type-ui';
import { OrderStatusBadgeComponent } from '../../shared/layout/order-status-badge.component';
import { orderReadyForBackendComplete } from '../../shared/order-ui';

@Component({
  selector: 'app-trip-edit',
  standalone: true,
  imports: [
    CommonModule,
    Button,
    Message,
    TableModule,
    TabsModule,
    OrderStatusBadgeComponent,
  ],
  templateUrl: './trip-edit.component.html',
  styleUrl: './trip-edit.component.css',
})
export class TripEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ordersApi = inject(OrderApiService);
  private readonly auditApi = inject(AuditApiService);
  private readonly confirm = inject(ConfirmationService);

  order: OrderResponse | null = null;
  auditEvents: AuditEventResponse[] = [];
  mainTab: string | number = 'overview';
  docs: OrderDocumentDescriptor[] = [];

  busy = false;
  /** Tracks in-flight document download for loading spinners. */
  downloadKey: string | null = null;
  conflictDetail: string | null = null;

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('orderId');
    if (!raw) {
      void this.router.navigateByUrl('/orders');
      return;
    }
    const id = Number(raw);
    if (!Number.isFinite(id)) {
      void this.router.navigateByUrl('/orders');
      return;
    }
    this.reload(id);
  }

  onMainTabChange(value: string | number): void {
    if (value === 'audit') {
      this.mainTab = 'audit';
    } else {
      this.mainTab = 'overview';
    }
  }

  reload(id: number): void {
    this.busy = true;
    this.conflictDetail = null;
    forkJoin({
      order: this.ordersApi.get(id),
      audit: this.auditApi.listByOrder(id),
    }).subscribe({
      next: ({ order, audit }) => {
        this.order = order;
        this.auditEvents = audit;
        this.refreshDocumentsIfNeeded(id);
        this.busy = false;
      },
      error: () => {
        this.busy = false;
        void this.router.navigateByUrl('/orders');
      },
    });
  }

  private refreshDocumentsIfNeeded(id: number): void {
    if (this.order?.completed) {
      this.ordersApi.listDocuments(id).subscribe({
        next: (d) => (this.docs = d),
        error: () => (this.docs = []),
      });
    } else {
      this.docs = [];
    }
  }

  get orderCompleted(): boolean {
    return this.order?.completed === true;
  }

  inProgress(): boolean {
    return !this.orderCompleted;
  }

  showDocuments(): boolean {
    return this.orderCompleted;
  }

  docTypeLabel(t: DocumentTypeName): string {
    return documentTypeLabelRu(t);
  }

  routeSnippet(o: OrderResponse): string {
    const a = (o.loadingPlace ?? '').trim();
    const b = (o.unloadingPlace ?? '').trim();
    const trunc = (s: string, n: number) =>
      s.length > n ? `${s.slice(0, n)}…` : s;
    if (!a && !b) {
      return '—';
    }
    return `${trunc(a, 48)} → ${trunc(b, 48)}`;
  }

  formatMoney(v: number | null): string {
    if (v === null || v === undefined) {
      return '—';
    }
    return new Intl.NumberFormat('ru-RU', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(v) + ' ₽';
  }

  get orderId(): number | null {
    return this.order?.id ?? null;
  }

  private handleSaveError(err: HttpErrorResponse): void {
    const ct = err.headers?.get('content-type') ?? '';
    if (
      err.status === 409 &&
      (ct.includes('application/problem+json') || ct.includes('application/json')) &&
      err.error &&
      typeof err.error === 'object'
    ) {
      const p = err.error as ProblemDetail;
      const { summary, detail } = localizeProblemToast(
        p.title,
        p.detail,
        err.status,
      );
      this.conflictDetail = detail || summary || 'Конфликт при сохранении';
      return;
    }
    this.conflictDetail = null;
  }

  private incompleteHintFromOrder(o: OrderResponse): string | null {
    if (o.customerId == null || o.performerId == null) {
      return 'В заказе не указаны заказчик и исполнитель.';
    }
    if (o.driverId == null || o.vehicleId == null) {
      return 'Укажите водителя и транспорт (через форму создания рейса или API).';
    }
    if (!(o.loadingPlace ?? '').trim() || !(o.unloadingPlace ?? '').trim()) {
      return 'Укажите адреса погрузки и выгрузки.';
    }
    if (!(o.orderDate ?? '').toString().trim()) {
      return 'Укажите дату заявки.';
    }
    if (o.totalPrice == null) {
      return 'Укажите итоговую сумму.';
    }
    return null;
  }

  complete(): void {
    const id = this.orderId;
    const o = this.order;
    if (!id || !o || !this.inProgress()) {
      return;
    }
    if (!orderReadyForBackendComplete(o)) {
      this.conflictDetail =
        this.incompleteHintFromOrder(o) ?? 'Рейс не готов к завершению.';
      return;
    }
    this.confirm.confirm({
      message: 'Завершить рейс? Будут сгенерированы документы.',
      header: 'Подтверждение',
      icon: 'pi pi-check-circle',
      accept: () => {
        this.busy = true;
        this.conflictDetail = null;
        this.ordersApi.complete(id).subscribe({
          next: (updated) => {
            this.order = updated;
            this.refreshDocumentsIfNeeded(id);
            this.auditApi.listByOrder(id).subscribe({
              next: (ev) => (this.auditEvents = ev),
              error: () => (this.auditEvents = []),
            });
            this.busy = false;
          },
          error: (err: HttpErrorResponse) => {
            this.handleSaveError(err);
            if (!this.conflictDetail) {
              this.conflictDetail = `Не удалось завершить рейс (код ${err.status}).`;
            }
            this.busy = false;
          },
        });
      },
    });
  }

  deleteOrder(): void {
    const id = this.orderId;
    if (!id) {
      return;
    }
    this.confirm.confirm({
      message: 'Удалить рейс? Это действие необратимо.',
      header: 'Подтверждение',
      icon: 'pi pi-trash',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.busy = true;
        this.ordersApi.delete(id).subscribe({
          next: () => {
            this.busy = false;
            void this.router.navigate(['/orders']);
          },
          error: () => {
            this.busy = false;
          },
        });
      },
    });
  }

  downloadDocument(docType: DocumentTypeName, format: FileFormatName): void {
    const id = this.orderId;
    if (!id) {
      return;
    }
    const key = `${docType}-${format}`;
    this.downloadKey = key;
    this.ordersApi
      .downloadDocument(id, docType, format)
      .pipe(finalize(() => (this.downloadKey = null)))
      .subscribe({
        next: ({ blob, fileName }) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = fileName;
          a.click();
          URL.revokeObjectURL(url);
        },
      });
  }

  downloadBundle(format: FileFormatName): void {
    const id = this.orderId;
    if (!id || !this.order) {
      return;
    }
    const key = `bundle-${format}`;
    this.downloadKey = key;
    this.ordersApi
      .downloadDocumentsBundle(id, format, {
        orderNumber: this.order.orderNumber,
        orderDate: this.order.orderDate,
      })
      .pipe(finalize(() => (this.downloadKey = null)))
      .subscribe({
        next: ({ blob, fileName }) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = fileName;
          a.click();
          URL.revokeObjectURL(url);
        },
      });
  }

  docDownloadKey(docType: DocumentTypeName, format: FileFormatName): string {
    return `${docType}-${format}`;
  }

  isDownloadBusy(key: string): boolean {
    return this.downloadKey === key;
  }

  eventTypeLabel(t: AuditEventResponse['eventType']): string {
    switch (t) {
      case 'ORDER_CREATED':
        return 'Рейс создан';
      case 'ORDER_UPDATED':
        return 'Изменение';
      case 'ORDER_COMPLETED':
        return 'Завершён';
      case 'ORDER_DELETED':
        return 'Удалён';
      case 'DOCUMENTS_GENERATED':
        return 'Документы сформированы';
      case 'LOGIN':
        return 'Вход';
      case 'REGISTER':
        return 'Регистрация';
      default:
        return t;
    }
  }

  prettyPayload(raw: string | null): string {
    if (!raw) {
      return '';
    }
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }
}
