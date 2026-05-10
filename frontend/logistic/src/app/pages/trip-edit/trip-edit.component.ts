import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { concatMap, forkJoin } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { Message } from 'primeng/message';
import { StepperModule } from 'primeng/stepper';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CustomerResponse,
  DriverResponse,
  PerformerResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { AuditApiService } from '../../core/audit-api.service';
import { AuditEventResponse } from '../../core/audit.models';
import { localizeProblemToast } from '../../core/error-messages';
import { OrderApiService } from '../../core/order-api.service';
import {
  DocumentTypeName,
  FileFormatName,
  OrderDocumentDescriptor,
  OrderResponse,
  OrderUpdateRequest,
} from '../../core/order.models';
import { ProblemDetail } from '../../models/problem.models';
import { OrderStatusBadgeComponent } from '../../shared/layout/order-status-badge.component';
import { OrderCatalogDialogsComponent } from '../../shared/order-catalog-dialogs/order-catalog-dialogs.component';
import { orderMarkedCompleted } from '../../shared/order-ui';

@Component({
  selector: 'app-trip-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    StepperModule,
    DropdownModule,
    DatePickerModule,
    InputText,
    InputTextarea,
    InputNumber,
    Button,
    Message,
    OrderCatalogDialogsComponent,
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
  private readonly catalog = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  order: OrderResponse | null = null;
  auditEvents: AuditEventResponse[] = [];
  mainTab: string | number = 'edit';
  docs: OrderDocumentDescriptor[] = [];
  customers: CustomerResponse[] = [];
  performers: PerformerResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  busy = false;
  stepperValue: number | undefined = 1;
  conflictDetail: string | null = null;

  readonly form = this.fb.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    performerId: this.fb.control<number | null>(null, Validators.required),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
    orderNumber: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
    ]),
    loadingPlace: ['', Validators.required],
    loadingContact: [''],
    unloadingPlace: ['', Validators.required],
    unloadingContact: [''],
    orderDate: this.fb.control<Date | null>(null, Validators.required),
    tripCount: this.fb.control<number>(1, [Validators.required, Validators.min(1)]),
    pricePerTrip: this.fb.control<number | null>(null, [Validators.min(0)]),
    totalPrice: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
  });

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

  reload(id: number): void {
    this.busy = true;
    this.conflictDetail = null;
    forkJoin({
      order: this.ordersApi.get(id),
      customers: this.catalog.listCustomers(),
      performers: this.catalog.listPerformers(),
      drivers: this.catalog.listDrivers(),
      vehicles: this.catalog.listVehicles(),
    }).subscribe({
      next: ({ order, customers, performers, drivers, vehicles }) => {
        this.order = order;
        this.customers = customers;
        this.performers = performers;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.patchForm(order);
        this.auditApi.listByOrder(id).subscribe({
          next: (ev) => {
            this.auditEvents = ev;
            this.refreshDocumentsIfNeeded(id);
          },
          error: () => {
            this.auditEvents = [];
            this.docs = [];
          },
        });
        this.busy = false;
      },
      error: () => {
        this.busy = false;
        void this.router.navigateByUrl('/orders');
      },
    });
  }

  /** Обновить справочники после создания/изменения записи в модалке. */
  onCatalogSaved(): void {
    forkJoin({
      customers: this.catalog.listCustomers(),
      performers: this.catalog.listPerformers(),
      drivers: this.catalog.listDrivers(),
      vehicles: this.catalog.listVehicles(),
    }).subscribe({
      next: ({ customers, performers, drivers, vehicles }) => {
        this.customers = customers;
        this.performers = performers;
        this.drivers = drivers;
        this.vehicles = vehicles;
      },
    });
  }

  private refreshDocumentsIfNeeded(id: number): void {
    if (this.orderCompleted) {
      this.ordersApi.listDocuments(id).subscribe({
        next: (d) => (this.docs = d),
        error: () => (this.docs = []),
      });
    } else {
      this.docs = [];
    }
  }

  get orderCompleted(): boolean {
    return orderMarkedCompleted(this.auditEvents);
  }

  private patchForm(t: OrderResponse): void {
    const d = t.orderDate
      ? new Date(t.orderDate.slice(0, 10) + 'T12:00:00')
      : null;
    this.form.patchValue({
      customerId: t.customerId,
      performerId: t.performerId,
      driverId: t.driverId,
      vehicleId: t.vehicleId,
      orderNumber: t.orderNumber,
      loadingPlace: t.loadingPlace ?? '',
      loadingContact: t.loadingContact ?? '',
      unloadingPlace: t.unloadingPlace ?? '',
      unloadingContact: t.unloadingContact ?? '',
      orderDate: d,
      tripCount: t.tripCount,
      pricePerTrip:
        t.pricePerTrip === null || t.pricePerTrip === undefined
          ? null
          : Number(t.pricePerTrip),
      totalPrice:
        t.totalPrice === null || t.totalPrice === undefined
          ? null
          : Number(t.totalPrice),
    });
  }

  private sortOpts<T extends { label: string }>(rows: T[]): T[] {
    return [...rows].sort((a, b) => a.label.localeCompare(b.label, 'ru'));
  }

  customerOptions(): { label: string; value: number }[] {
    return this.sortOpts(
      this.customers.map((c) => ({ label: c.shortName, value: c.id })),
    );
  }

  performerOptions(): { label: string; value: number }[] {
    return this.sortOpts(
      this.performers.map((p) => ({ label: p.shortName, value: p.id })),
    );
  }

  driverOptions(): { label: string; value: number }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.drivers
        : this.drivers.filter((d) => d.performerId === pid);
    return this.sortOpts(
      list.map((d) => ({
        label: d.fullName,
        value: d.id,
      })),
    );
  }

  vehicleOptions(): { label: string; value: number }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.vehicles
        : this.vehicles.filter((v) => v.performerId === pid);
    return this.sortOpts(
      list.map((v) => ({
        label: `${v.plateNumber ?? ''}${v.brandModel ? ' · ' + v.brandModel : ''}`,
        value: v.id,
      })),
    );
  }

  private toIsoDate(d: Date | null | undefined): string | undefined {
    if (!d) {
      return undefined;
    }
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private buildBody(): OrderUpdateRequest {
    const v = this.form.getRawValue();
    return {
      customerId: v.customerId ?? undefined,
      performerId: v.performerId ?? undefined,
      vehicleId: v.vehicleId ?? undefined,
      driverId: v.driverId ?? undefined,
      orderNumber: v.orderNumber ?? undefined,
      orderDate: this.toIsoDate(v.orderDate),
      loadingPlace: (v.loadingPlace ?? '').trim(),
      loadingContact: (v.loadingContact ?? '').trim() || undefined,
      unloadingPlace: (v.unloadingPlace ?? '').trim(),
      unloadingContact: (v.unloadingContact ?? '').trim() || undefined,
      tripCount: v.tripCount ?? undefined,
      pricePerTrip: v.pricePerTrip ?? undefined,
      totalPrice: v.totalPrice ?? undefined,
    };
  }

  private incompleteHint(): string | null {
    const v = this.form.getRawValue();
    if (v.customerId == null || v.performerId == null) {
      return 'Выберите заказчика и исполнителя.';
    }
    if (v.driverId == null || v.vehicleId == null) {
      return 'Выберите водителя и транспорт.';
    }
    if (!(v.loadingPlace ?? '').trim() || !(v.unloadingPlace ?? '').trim()) {
      return 'Укажите адреса погрузки и выгрузки.';
    }
    if (v.orderDate == null) {
      return 'Укажите дату заявки.';
    }
    if (v.totalPrice == null) {
      return 'Укажите итоговую сумму.';
    }
    return null;
  }

  inProgress(): boolean {
    return !this.orderCompleted;
  }

  canEditCatalog(): boolean {
    return true;
  }

  showDocuments(): boolean {
    return this.orderCompleted;
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

  save(): void {
    const id = this.orderId;
    if (!id || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy = true;
    this.conflictDetail = null;
    this.ordersApi.update(id, this.buildBody()).subscribe({
      next: (o) => {
        this.order = o;
        this.patchForm(o);
        this.refreshDocumentsIfNeeded(id);
        this.busy = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleSaveError(err);
        this.busy = false;
      },
    });
  }

  complete(): void {
    const id = this.orderId;
    if (!id || !this.inProgress()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const hint = this.incompleteHint();
    if (hint) {
      this.conflictDetail = hint;
      return;
    }
    this.confirm.confirm({
      message: 'Завершить рейс? Будут сгенерированы документы.',
      header: 'Подтверждение',
      icon: 'pi pi-check-circle',
      accept: () => {
        this.busy = true;
        this.conflictDetail = null;
        this.ordersApi
          .update(id, this.buildBody())
          .pipe(concatMap(() => this.ordersApi.complete(id)))
          .subscribe({
            next: () => {
              this.reload(id);
              this.busy = false;
            },
            error: (err: HttpErrorResponse) => {
              this.handleSaveError(err);
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
    this.ordersApi.downloadDocument(id, docType, format).subscribe({
      next: (blob) => {
        const ext = format === 'PDF' ? 'pdf' : 'docx';
        const name = `${docType.toLowerCase()}.${ext}`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  customerSelected(): boolean {
    return this.form.getRawValue().customerId != null;
  }

  performerSelected(): boolean {
    return this.form.getRawValue().performerId != null;
  }

  driverSelected(): boolean {
    return this.form.getRawValue().driverId != null;
  }

  vehicleSelected(): boolean {
    return this.form.getRawValue().vehicleId != null;
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
