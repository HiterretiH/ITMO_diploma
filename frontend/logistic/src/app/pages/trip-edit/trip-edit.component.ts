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
import { Dialog } from 'primeng/dialog';
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
import { innValidator } from '../../shared/forms/inn.validator';
import { plateValidator } from '../../shared/forms/plate.validator';
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
    Dialog,
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

  customerDialogVisible = false;
  customerSaving = false;
  customerEditingId: number | null = null;
  readonly customerForm = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    requisites: [''],
  });

  performerDialogVisible = false;
  performerSaving = false;
  performerEditingId: number | null = null;
  readonly performerForm = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    inn: ['', [innValidator]],
  });

  driverDialogVisible = false;
  driverEditingId: number | null = null;
  driverSaving = false;
  readonly driverForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(1)]],
    phone: [''],
    isDefault: [false],
  });

  vehicleDialogVisible = false;
  vehicleEditingId: number | null = null;
  vehicleSaving = false;
  readonly vehicleForm = this.fb.group({
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    brandModel: [''],
    type: [''],
    isDefault: this.fb.control(false),
  });

  readonly form = this.fb.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    performerId: this.fb.control<number | null>(null, Validators.required),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
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

  customerOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.customers.map((c) => ({
        label: c.shortName,
        value: c.id,
      })),
    ];
  }

  performerOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.performers.map((p) => ({
        label: p.shortName,
        value: p.id,
      })),
    ];
  }

  driverOptions(): { label: string; value: number | null }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.drivers
        : this.drivers.filter((d) => d.performerId === pid);
    return [
      { label: '—', value: null },
      ...list.map((d) => ({
        label: d.fullName,
        value: d.id,
      })),
    ];
  }

  vehicleOptions(): { label: string; value: number | null }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.vehicles
        : this.vehicles.filter((v) => v.performerId === pid);
    return [
      { label: '—', value: null },
      ...list.map((v) => ({
        label: `${v.plateNumber ?? ''}${v.brandModel ? ' · ' + v.brandModel : ''}`,
        value: v.id,
      })),
    ];
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
      message: 'Завершить заказ? Будут сгенерированы документы.',
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
      message: 'Удалить заказ? Это действие необратимо.',
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

  openCustomerCreate(): void {
    this.customerEditingId = null;
    this.customerForm.reset({
      shortName: '',
      fullName: '',
      phone: '',
      requisites: '',
    });
    this.customerDialogVisible = true;
  }

  openCustomerEdit(): void {
    const id = this.form.getRawValue().customerId;
    if (id == null) {
      return;
    }
    const c = this.customers.find((x) => x.id === id);
    if (!c) {
      return;
    }
    this.customerEditingId = c.id;
    this.customerForm.setValue({
      shortName: c.shortName,
      fullName: c.fullName ?? '',
      phone: c.phone ?? '',
      requisites: c.requisites ?? '',
    });
    this.customerDialogVisible = true;
  }

  openPerformerCreate(): void {
    this.performerEditingId = null;
    this.performerForm.reset({
      shortName: '',
      fullName: '',
      phone: '',
      inn: '',
    });
    this.performerDialogVisible = true;
  }

  openPerformerEdit(): void {
    const id = this.form.getRawValue().performerId;
    if (id == null) {
      return;
    }
    const p = this.performers.find((x) => x.id === id);
    if (!p) {
      return;
    }
    this.performerEditingId = p.id;
    this.performerForm.setValue({
      shortName: p.shortName,
      fullName: p.fullName ?? '',
      phone: p.phone ?? '',
      inn: p.inn ?? '',
    });
    this.performerDialogVisible = true;
  }

  openDriverCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.conflictDetail = 'Сначала выберите исполнителя.';
      return;
    }
    this.driverEditingId = null;
    this.driverForm.reset({
      fullName: '',
      phone: '',
      isDefault: false,
    });
    this.driverDialogVisible = true;
  }

  openDriverEdit(): void {
    const id = this.form.getRawValue().driverId;
    if (id == null) {
      return;
    }
    const d = this.drivers.find((x) => x.id === id);
    if (!d) {
      return;
    }
    this.driverEditingId = d.id;
    this.driverForm.setValue({
      fullName: d.fullName,
      phone: d.phone ?? '',
      isDefault: d.isDefault,
    });
    this.driverDialogVisible = true;
  }

  openVehicleCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.conflictDetail = 'Сначала выберите исполнителя.';
      return;
    }
    this.vehicleEditingId = null;
    this.vehicleForm.reset({
      plateNumber: '',
      brandModel: '',
      type: '',
      isDefault: false,
    });
    this.vehicleDialogVisible = true;
  }

  openVehicleEdit(): void {
    const id = this.form.getRawValue().vehicleId;
    if (id == null) {
      return;
    }
    const v = this.vehicles.find((x) => x.id === id);
    if (!v) {
      return;
    }
    this.vehicleEditingId = v.id;
    this.vehicleForm.setValue({
      plateNumber: v.plateNumber ?? '',
      brandModel: v.brandModel ?? '',
      type: v.type ?? '',
      isDefault: v.isDefault,
    });
    this.vehicleDialogVisible = true;
  }

  saveCustomerDialog(): void {
    if (this.customerForm.invalid || this.customerSaving) {
      this.customerForm.markAllAsTouched();
      return;
    }
    const v = this.customerForm.getRawValue();
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      requisites: v.requisites.trim() === '' ? null : v.requisites.trim(),
    };
    this.customerSaving = true;
    const obs =
      this.customerEditingId != null
        ? this.catalog.updateCustomer(this.customerEditingId, body)
        : this.catalog.createCustomer(body);
    obs.subscribe({
      next: (c) => {
        if (this.customerEditingId != null) {
          this.customers = this.customers
            .map((x) => (x.id === c.id ? c : x))
            .sort((a, b) => a.shortName.localeCompare(b.shortName));
        } else {
          this.customers = [...this.customers, c].sort((a, b) =>
            a.shortName.localeCompare(b.shortName),
          );
          this.form.patchValue({ customerId: c.id });
        }
        this.customerSaving = false;
        this.customerDialogVisible = false;
      },
      error: () => {
        this.customerSaving = false;
      },
    });
  }

  savePerformerDialog(): void {
    if (this.performerForm.invalid || this.performerSaving) {
      this.performerForm.markAllAsTouched();
      return;
    }
    const v = this.performerForm.getRawValue();
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      inn: v.inn.trim() === '' ? null : v.inn.trim(),
    };
    this.performerSaving = true;
    const obs =
      this.performerEditingId != null
        ? this.catalog.updatePerformer(this.performerEditingId, body)
        : this.catalog.createPerformer(body);
    obs.subscribe({
      next: (p) => {
        if (this.performerEditingId != null) {
          this.performers = this.performers
            .map((x) => (x.id === p.id ? p : x))
            .sort((a, b) => a.shortName.localeCompare(b.shortName));
        } else {
          this.performers = [...this.performers, p].sort((a, b) =>
            a.shortName.localeCompare(b.shortName),
          );
          this.form.patchValue({ performerId: p.id });
        }
        this.performerSaving = false;
        this.performerDialogVisible = false;
      },
      error: () => {
        this.performerSaving = false;
      },
    });
  }

  saveDriverDialog(): void {
    const performerId = this.form.getRawValue().performerId;
    if (performerId == null || this.driverForm.invalid || this.driverSaving) {
      this.driverForm.markAllAsTouched();
      return;
    }
    const v = this.driverForm.getRawValue();
    const body = {
      performerId,
      fullName: v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      isDefault: v.isDefault,
    };
    this.driverSaving = true;
    const obs =
      this.driverEditingId != null
        ? this.catalog.updateDriver(this.driverEditingId, body)
        : this.catalog.createDriver(body);
    obs.subscribe({
      next: (d) => {
        if (this.driverEditingId != null) {
          this.drivers = this.drivers
            .map((x) => (x.id === d.id ? d : x))
            .sort((a, b) => a.fullName.localeCompare(b.fullName));
        } else {
          this.drivers = [...this.drivers, d].sort((a, b) =>
            a.fullName.localeCompare(b.fullName),
          );
          this.form.patchValue({ driverId: d.id });
        }
        this.driverSaving = false;
        this.driverDialogVisible = false;
      },
      error: () => {
        this.driverSaving = false;
      },
    });
  }

  saveVehicleDialog(): void {
    const performerId = this.form.getRawValue().performerId;
    if (performerId == null || this.vehicleForm.invalid || this.vehicleSaving) {
      this.vehicleForm.markAllAsTouched();
      return;
    }
    const v = this.vehicleForm.getRawValue();
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const body = {
      performerId,
      plateNumber: plate === '' ? null : plate,
      brandModel:
        (v.brandModel ?? '').trim() === '' ? null : (v.brandModel ?? '').trim(),
      type: (v.type ?? '').trim() === '' ? null : (v.type ?? '').trim(),
      isDefault: !!v.isDefault,
    };
    this.vehicleSaving = true;
    const obs =
      this.vehicleEditingId != null
        ? this.catalog.updateVehicle(this.vehicleEditingId, body)
        : this.catalog.createVehicle(body);
    obs.subscribe({
      next: (ve) => {
        if (this.vehicleEditingId != null) {
          this.vehicles = this.vehicles
            .map((x) => (x.id === ve.id ? ve : x))
            .sort((a, b) =>
              (a.plateNumber ?? '').localeCompare(b.plateNumber ?? ''),
            );
        } else {
          this.vehicles = [...this.vehicles, ve].sort((a, b) =>
            (a.plateNumber ?? '').localeCompare(b.plateNumber ?? ''),
          );
          this.form.patchValue({ vehicleId: ve.id });
        }
        this.vehicleSaving = false;
        this.vehicleDialogVisible = false;
      },
      error: () => {
        this.vehicleSaving = false;
      },
    });
  }

  eventTypeLabel(t: AuditEventResponse['eventType']): string {
    switch (t) {
      case 'ORDER_CREATED':
        return 'Заказ создан';
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
