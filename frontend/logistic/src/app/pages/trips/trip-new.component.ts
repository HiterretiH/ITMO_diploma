import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { concatMap, distinctUntilChanged, forkJoin } from 'rxjs';
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
import { OrderUpdateRequest, TripFormDraftResponse } from '../../core/order.models';
import { ProblemDetail } from '../../models/problem.models';
import { OrderCatalogDialogsComponent } from '../../shared/order-catalog-dialogs/order-catalog-dialogs.component';

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
  ],
  templateUrl: './trip-new.component.html',
  styleUrl: './trip-new.component.css',
})
export class TripNewComponent implements OnInit {
  private readonly orders = inject(OrderApiService);
  private readonly catalog = inject(CatalogApiService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

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
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    this.form.patchValue({
      orderDate: today,
    });

    forkJoin({
      customers: this.catalog.listCustomers(),
      performers: this.catalog.listPerformers(),
      drivers: this.catalog.listDrivers(),
      vehicles: this.catalog.listVehicles(),
      draft: this.orders.getTripFormDraft(),
    }).subscribe({
      next: ({ customers, performers, drivers, vehicles, draft }) => {
        this.customers = customers;
        this.performers = performers;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.applyTripDraft(draft);
      },
      error: () => {
        this.errorMessage =
          'Не удалось загрузить справочники. Проверьте доступ к серверу и обновите страницу.';
      },
    });
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

  /** Создаёт рейс и переходит на карточку без завершения (черновик с полными данными формы). */
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

    const v = this.form.getRawValue();
    const body = this.buildUpdateRequest();
    this.busy = true;
    this.orders
      .create({
        customerId: v.customerId!,
        performerId: v.performerId!,
        vehicleId: v.vehicleId,
        driverId: v.driverId,
      })
      .pipe(concatMap((created) => this.orders.update(created.id, body)))
      .subscribe({
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

  saveAndComplete(): void {
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

    const v = this.form.getRawValue();
    const body = this.buildUpdateRequest();
    this.busy = true;
    this.orders
      .create({
        customerId: v.customerId!,
        performerId: v.performerId!,
        vehicleId: v.vehicleId,
        driverId: v.driverId,
      })
      .pipe(
        concatMap((order) =>
          this.orders.update(order.id, body).pipe(
            concatMap(() => this.orders.complete(order.id)),
          ),
        ),
      )
      .subscribe({
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
}
