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
import { concatMap, forkJoin } from 'rxjs';
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
import { OrderUpdateRequest } from '../../core/order.models';
import { ProblemDetail } from '../../models/problem.models';

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
    loadingPlace: ['', Validators.required],
    loadingContact: [''],
    unloadingPlace: ['', Validators.required],
    unloadingContact: [''],
    orderDate: this.fb.control<Date | null>(null, Validators.required),
    legCount: this.fb.control<number>(1, [Validators.required, Validators.min(1)]),
    ratePerLeg: this.fb.control<number | null>(0, [Validators.min(0)]),
    priceAmount: this.fb.control<number | null>(0, [
      Validators.required,
      Validators.min(0),
    ]),
  });

  constructor() {
    this.form.controls.ratePerLeg.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());
    this.form.controls.legCount.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());
  }

  private applyTotalFromRateAndLegs(): void {
    const v = this.form.getRawValue();
    const rate = Number(v.ratePerLeg ?? 0);
    const n = Number(v.legCount ?? 1);
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
    }).subscribe({
      next: ({ customers, performers, drivers, vehicles }) => {
        this.customers = customers;
        this.performers = performers;
        this.drivers = drivers;
        this.vehicles = vehicles;
      },
      error: () => {
        this.errorMessage =
          'Не удалось загрузить справочники. Проверьте доступ к API и обновите страницу.';
      },
    });
  }

  customerOptions(): { label: string; value: number | null }[] {
    return [
      { label: 'Выберите заказчика…', value: null },
      ...this.customers.map((c) => ({ label: c.shortName, value: c.id })),
    ];
  }

  performerOptions(): { label: string; value: number | null }[] {
    return [
      { label: 'Выберите исполнителя…', value: null },
      ...this.performers.map((p) => ({ label: p.shortName, value: p.id })),
    ];
  }

  driverOptions(): { label: string; value: number | null }[] {
    const pid = this.form.getRawValue().performerId;
    const list =
      pid == null
        ? this.drivers
        : this.drivers.filter((d) => d.performerId === pid);
    return [
      { label: 'Выберите водителя…', value: null },
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
      { label: 'Выберите ТС…', value: null },
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

  private buildUpdateRequest(): OrderUpdateRequest {
    const v = this.form.getRawValue();
    return {
      loadingPlace: (v.loadingPlace ?? '').trim(),
      loadingContact: (v.loadingContact ?? '').trim() || undefined,
      unloadingPlace: (v.unloadingPlace ?? '').trim(),
      unloadingContact: (v.unloadingContact ?? '').trim() || undefined,
      orderDate: this.toIsoDate(v.orderDate),
      tripCount: v.legCount ?? undefined,
      pricePerTrip: v.ratePerLeg ?? undefined,
      totalPrice: v.priceAmount ?? undefined,
      driverId: v.driverId ?? undefined,
      vehicleId: v.vehicleId ?? undefined,
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
      return 'Укажите итоговую сумму по заявке.';
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
