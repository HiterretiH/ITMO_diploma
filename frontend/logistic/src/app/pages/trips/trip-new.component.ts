import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, forkJoin, of } from 'rxjs';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { Divider } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { Message } from 'primeng/message';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CustomerResponse,
  DriverResponse,
  PerformerResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { localizeProblemToast } from '../../core/error-messages';
import { OrderApiService } from '../../core/order-api.service';
import {
  OrderCreateRequest,
  OrderResponse,
  OrderUpdateRequest,
  TripFormDraftResponse,
} from '../../core/order.models';
import { ProblemDetail } from '../../models/problem.models';
import { OrderCatalogDialogsComponent } from '../../shared/order-catalog-dialogs/order-catalog-dialogs.component';
import { TripFormFieldComponent } from '../../shared/trip-form-field/trip-form-field.component';

@Component({
  selector: 'app-trip-new',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    Card,
    Divider,
    DropdownModule,
    DatePickerModule,
    InputNumber,
    InputText,
    InputTextarea,
    Button,
    Message,
    OrderCatalogDialogsComponent,
    TripFormFieldComponent,
  ],
  templateUrl: './trip-new.component.html',
  styleUrl: './trip-new.component.css',
})
export class TripNewComponent implements OnInit {
  private readonly orders = inject(OrderApiService);
  private readonly catalog = inject(CatalogApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  /** When set, form edits an existing trip (PUT) instead of create (POST). */
  editOrderId: number | null = null;

  customers: CustomerResponse[] = [];
  performers: PerformerResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  busy = false;
  errorMessage: string | null = null;

  readonly form = this.fb.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    performerId: this.fb.control<number | null>(null, Validators.required),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
    orderNumber: this.fb.control<number | null>(null),
    loadingPlace: ['', Validators.required],
    loadingContact: [''],
    unloadingPlace: ['', Validators.required],
    unloadingContact: [''],
    orderDate: this.fb.control<Date | null>(null, Validators.required),
    legCount: this.fb.control<number | null>(1, [
      Validators.required,
      Validators.min(1),
    ]),
    ratePerLeg: this.fb.control<number | null>(null, [Validators.min(0)]),
    priceAmount: this.fb.control<number | null>(null, [Validators.min(0)]),
  });

  constructor() {
    this.form.controls.ratePerLeg.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());
    this.form.controls.legCount.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());

    this.form.controls.customerId.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((customerId) => {
        if (this.editOrderId != null) {
          return;
        }
        this.orders.getTripFormDraft(customerId ?? undefined).subscribe({
          next: (d) =>
            this.form.patchValue(
              { orderNumber: d.nextOrderNumber },
              { emitEvent: false },
            ),
          error: () => {},
        });
      });
  }

  private applyTotalFromRateAndLegs(): void {
    const v = this.form.getRawValue();
    const rawRate = v.ratePerLeg;
    const n = Number(v.legCount ?? 1);
    if (
      rawRate === null ||
      rawRate === undefined ||
      !Number.isFinite(Number(rawRate))
    ) {
      return;
    }
    const rate = Number(rawRate);
    this.form.patchValue({ priceAmount: rate * n }, { emitEvent: false });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('orderId');
    if (this.router.url.includes('/edit') && idParam) {
      this.editOrderId = Number(idParam);
    }

    forkJoin({
      customers: this.catalog.listCustomers(),
      performers: this.catalog.listPerformers(),
      drivers: this.catalog.listDrivers(),
      vehicles: this.catalog.listVehicles(),
      draft: this.orders.getTripFormDraft(),
      existing:
        this.editOrderId != null
          ? this.orders.get(this.editOrderId)
          : of(null as OrderResponse | null),
    }).subscribe({
      next: ({ customers, performers, drivers, vehicles, draft, existing }) => {
        this.customers = customers;
        this.performers = performers;
        this.drivers = drivers;
        this.vehicles = vehicles;
        if (existing) {
          this.patchOrderIntoForm(existing);
        } else {
          const today = new Date();
          today.setHours(12, 0, 0, 0);
          this.form.patchValue({
            orderDate: today,
          });
          this.applyTripDraft(draft);
        }
      },
      error: () => {
        this.errorMessage =
          'Не удалось загрузить справочники. Проверьте доступ к серверу и обновите страницу.';
      },
    });
  }

  private patchOrderIntoForm(o: OrderResponse): void {
    const datePart = (o.orderDate ?? '').slice(0, 10);
    const orderDate =
      datePart.length >= 10
        ? new Date(`${datePart}T12:00:00`)
        : null;
    this.form.patchValue(
      {
        customerId: o.customerId,
        performerId: o.performerId,
        driverId: o.driverId,
        vehicleId: o.vehicleId,
        orderNumber: o.orderNumber,
        loadingPlace: o.loadingPlace ?? '',
        loadingContact: o.loadingContact ?? '',
        unloadingPlace: o.unloadingPlace ?? '',
        unloadingContact: o.unloadingContact ?? '',
        orderDate,
        legCount: o.tripCount,
        ratePerLeg: o.pricePerTrip,
        priceAmount: o.totalPrice,
      },
      { emitEvent: false },
    );
  }

  private applyTripDraft(draft: TripFormDraftResponse): void {
    this.form.patchValue(
      {
        orderNumber: draft.nextOrderNumber,
      },
      { emitEvent: false },
    );
    if (
      draft.lastPerformerId != null &&
      this.performers.some((p) => p.id === draft.lastPerformerId)
    ) {
      this.form.patchValue(
        { performerId: draft.lastPerformerId },
        { emitEvent: false },
      );
    }
    const perfId = this.form.getRawValue().performerId;
    if (perfId != null) {
      const pv: { driverId?: number | null; vehicleId?: number | null } = {};
      if (
        draft.lastDriverId != null &&
        this.drivers.some(
          (d) =>
            d.id === draft.lastDriverId && d.performerId === perfId,
        )
      ) {
        pv.driverId = draft.lastDriverId;
      }
      if (
        draft.lastVehicleId != null &&
        this.vehicles.some(
          (v) =>
            v.id === draft.lastVehicleId && v.performerId === perfId,
        )
      ) {
        pv.vehicleId = draft.lastVehicleId;
      }
      this.form.patchValue(pv, { emitEvent: false });
    }
  }

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
      error: () => {
        this.errorMessage =
          'Не удалось обновить справочники. Обновите страницу.';
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

  fieldAriaDescribedBy(errorDomId: string, c: AbstractControl): string | null {
    return c.invalid && c.touched ? errorDomId : null;
  }

  ratePerLegErrorText(): string {
    const c = this.form.controls.ratePerLeg;
    return c.hasError('min')
      ? 'Не может быть отрицательным'
      : 'Обязательное поле';
  }

  legCountErrorText(): string {
    const c = this.form.controls.legCount;
    return c.hasError('min') ? 'Минимум 1' : 'Обязательное поле';
  }

  priceAmountErrorText(): string {
    return 'Не может быть отрицательным';
  }

  private sortOpts<T extends { label: string }>(rows: T[]): T[] {
    return [...rows].sort((a, b) => a.label.localeCompare(b.label, 'ru'));
  }

  customerOptions(): { label: string; value: number }[] {
    const rows = this.customers.map((c) => ({
      label: c.shortName,
      value: c.id,
    }));
    return this.sortOpts(rows);
  }

  performerOptions(): { label: string; value: number }[] {
    const rows = this.performers.map((p) => ({
      label: p.shortName,
      value: p.id,
    }));
    return this.sortOpts(rows);
  }

  driverOptions(): { label: string; value: number }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.drivers
        : this.drivers.filter((d) => d.performerId === pid);
    const rows = list.map((d) => ({
      label: d.fullName,
      value: d.id,
    }));
    return this.sortOpts(rows);
  }

  vehicleOptions(): { label: string; value: number }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.vehicles
        : this.vehicles.filter((v) => v.performerId === pid);
    const rows = list.map((v) => {
      const plate = v.plateNumber ?? '';
      const bm = v.brandModel ? ` · ${v.brandModel}` : '';
      return {
        label: `${plate}${bm}`,
        value: v.id,
      };
    });
    return this.sortOpts(rows);
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

  private buildUpdateRequest(): OrderUpdateRequest {
    const v = this.form.getRawValue();
    const req: OrderUpdateRequest = {
      loadingPlace: (v.loadingPlace ?? '').trim(),
      loadingContact: (v.loadingContact ?? '').trim() || undefined,
      unloadingPlace: (v.unloadingPlace ?? '').trim(),
      unloadingContact: (v.unloadingContact ?? '').trim() || undefined,
      orderDate: this.toIsoDate(v.orderDate),
      tripCount: v.legCount ?? undefined,
      driverId: v.driverId ?? undefined,
      vehicleId: v.vehicleId ?? undefined,
    };
    if (
      v.ratePerLeg != null &&
      Number.isFinite(Number(v.ratePerLeg))
    ) {
      req.pricePerTrip = Number(v.ratePerLeg);
    }
    if (
      v.priceAmount != null &&
      Number.isFinite(Number(v.priceAmount))
    ) {
      req.totalPrice = Number(v.priceAmount);
    }
    const num = v.orderNumber;
    if (num != null && Number.isFinite(num) && num >= 1) {
      req.orderNumber = Math.floor(num);
    }
    return req;
  }

  private buildFullCreateRequest(): OrderCreateRequest {
    const v = this.form.getRawValue();
    const u = this.buildUpdateRequest();
    return {
      customerId: v.customerId!,
      performerId: v.performerId!,
      vehicleId: v.vehicleId ?? undefined,
      driverId: v.driverId ?? undefined,
      orderDate: u.orderDate,
      orderNumber: u.orderNumber,
      loadingPlace: u.loadingPlace,
      loadingContact: u.loadingContact,
      unloadingPlace: u.unloadingPlace,
      unloadingContact: u.unloadingContact,
      tripCount: u.tripCount,
      pricePerTrip: u.pricePerTrip,
      totalPrice: u.totalPrice ?? undefined,
    };
  }

  private incompleteHint(): string | null {
    const v = this.form.getRawValue();
    if (v.customerId == null || v.performerId == null) {
      return 'Выберите заказчика и исполнителя.';
    }
    if (v.driverId == null || v.vehicleId == null) {
      return 'Выберите водителя и транспортное средство.';
    }
    if (!(v.loadingPlace ?? '').trim() || !(v.unloadingPlace ?? '').trim()) {
      return 'Заполните адреса погрузки и выгрузки.';
    }
    if (v.orderDate == null) {
      return 'Укажите дату.';
    }
    if (v.priceAmount == null || Number(v.priceAmount) < 0) {
      return 'Укажите итоговую сумму по рейсу.';
    }
    return null;
  }

  private handleError(err: HttpErrorResponse): void {
    const ct = err.headers?.get('content-type') ?? '';
    if (
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
      this.errorMessage = detail || summary || `Ошибка ${err.status}`;
      return;
    }
    this.errorMessage = `Запрос не выполнен (код ${err.status}).`;
  }

  /** Создаёт или обновляет рейс одним запросом и переходит на карточку. */
  saveDraft(): void {
    this.errorMessage = null;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.errorMessage = 'Проверьте обязательные поля.';
      return;
    }
    const hint = this.incompleteHint();
    if (hint) {
      this.errorMessage = hint;
      return;
    }

    this.busy = true;
    const id = this.editOrderId;
    const req$ =
      id != null
        ? this.orders.update(id, this.buildUpdateRequest())
        : this.orders.create(this.buildFullCreateRequest());
    req$.subscribe({
      next: (o) => {
        this.busy = false;
        void this.router.navigate(['/orders', o.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err);
        this.busy = false;
      },
    });
  }

  get isEditMode(): boolean {
    return this.editOrderId != null;
  }
}
