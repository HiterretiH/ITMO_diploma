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
import { Message } from 'primeng/message';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { localizeProblemToast } from '../../core/error-messages';
import { TripApiService } from '../../core/trip-api.service';
import { TripUpdateRequest } from '../../core/trip.models';
import { ProblemDetail } from '../../models/problem.models';
import { PlaceInputComponent } from '../../shared/forms/place-input.component';
import { buildRouteLine } from '../../shared/forms/route-line.util';

/**
 * Создание рейса: данные как в `.ide/main.py` (стороны, маршрут+контакты, груз,
 * дата, ставка × количество → итог с возможностью ручной правки), затем POST → PUT → submit.
 *
 * Связность полей (аналог десктопа):
 * — ставка за рейс и число рейсов пересчитывают итог; итог можно править вручную;
 * — при смене даты погрузки дата разгрузки не остаётся «раньше» погрузки;
 * — водитель и ТС в десктопе задавались одной строкой «исполнителя»; в API они
 *   разделены — пользователь выбирает оба поля (порядок любой).
 */
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
    InputText,
    InputNumber,
    Button,
    Message,
    PlaceInputComponent,
  ],
  templateUrl: './trip-new.component.html',
  styleUrl: './trip-new.component.css',
})
export class TripNewComponent implements OnInit {
  private readonly trips = inject(TripApiService);
  private readonly catalog = inject(CatalogApiService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  counterparties: CounterpartyResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  busy = false;
  errorMessage: string | null = null;

  readonly form = this.fb.group({
    shipperId: this.fb.control<number | null>(null),
    consigneeId: this.fb.control<number | null>(null),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
    originAddress: ['', [Validators.required]],
    originContact: [''],
    destinationAddress: ['', [Validators.required]],
    destinationContact: [''],
    cargoDescription: ['', [Validators.required]],
    cargoWeightKg: this.fb.control<number | null>(null, [Validators.min(0)]),
    loadDate: this.fb.control<Date | null>(null, Validators.required),
    unloadDate: this.fb.control<Date | null>(null, Validators.required),
    legCount: this.fb.control<number>(1, [Validators.required, Validators.min(1)]),
    ratePerLeg: this.fb.control<number | null>(0, [Validators.min(0)]),
    /** Как `total_price` в `.ide/main.py`: редактируется вручную; rate×count пересчитывает до следующей ручной правки. */
    priceAmount: this.fb.control<number | null>(0, [
      Validators.required,
      Validators.min(0),
    ]),
  });

  constructor() {
    this.form.controls.loadDate.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((load) => {
        const unload = this.form.controls.unloadDate.value;
        if (!load || !unload) {
          return;
        }
        if (this.startOfDay(unload) < this.startOfDay(load)) {
          const synced = new Date(load.getTime());
          synced.setHours(12, 0, 0, 0);
          this.form.patchValue({ unloadDate: synced }, { emitEvent: false });
        }
      });

    this.form.controls.ratePerLeg.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());
    this.form.controls.legCount.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.applyTotalFromRateAndLegs());
  }

  /** Авто-заполнение итога (симметрично rate×count; в main.py пересчёт только на FocusOut цены). */
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
      loadDate: today,
      unloadDate: new Date(today.getTime()),
    });

    forkJoin({
      cp: this.catalog.counterparties(),
      dr: this.catalog.drivers(),
      ve: this.catalog.vehicles(),
    }).subscribe({
      next: ({ cp, dr, ve }) => {
        this.counterparties = cp;
        this.drivers = dr;
        this.vehicles = ve;
      },
      error: () => {
        this.errorMessage =
          'Не удалось загрузить справочники. Проверьте доступ к API и обновите страницу.';
      },
    });
  }

  cpOptions(): { label: string; value: number | null }[] {
    return [
      { label: 'Выберите организацию…', value: null },
      ...this.counterparties.map((c) => ({ label: c.name, value: c.id })),
    ];
  }

  driverOptions(): { label: string; value: number | null }[] {
    return [
      { label: 'Выберите водителя…', value: null },
      ...this.drivers.map((d) => ({
        label: d.fullName,
        value: d.id,
      })),
    ];
  }

  vehicleOptions(): { label: string; value: number | null }[] {
    return [
      { label: 'Выберите ТС…', value: null },
      ...this.vehicles.map((v) => ({
        label: `${v.plateNumber}${v.model ? ' · ' + v.model : ''}`,
        value: v.id,
      })),
    ];
  }

  private startOfDay(d: Date): number {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  }

  private toIsoDate(d: Date | null | undefined): string | null {
    if (!d) {
      return null;
    }
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private buildUpdateRequest(): TripUpdateRequest {
    const v = this.form.getRawValue();
    return {
      shipperId: v.shipperId,
      consigneeId: v.consigneeId,
      driverId: v.driverId,
      vehicleId: v.vehicleId,
      cargoDescription: (v.cargoDescription ?? '').trim() || null,
      cargoWeightKg: v.cargoWeightKg,
      routeFrom: buildRouteLine(v.originAddress, v.originContact),
      routeTo: buildRouteLine(v.destinationAddress, v.destinationContact),
      loadDate: this.toIsoDate(v.loadDate),
      unloadDate: this.toIsoDate(v.unloadDate),
      priceAmount: v.priceAmount ?? 0,
      currency: 'RUB',
    };
  }

  private incompleteHint(): string | null {
    const v = this.form.getRawValue();
    if (
      v.shipperId == null ||
      v.consigneeId == null ||
      v.driverId == null ||
      v.vehicleId == null
    ) {
      return 'Выберите отправителя и получателя груза, водителя и транспортное средство.';
    }
    if (!(v.cargoDescription ?? '').trim()) {
      return 'Укажите наименование или описание груза.';
    }
    if (v.cargoWeightKg == null) {
      return 'Укажите массу груза в килограммах.';
    }
    if (!(v.originAddress ?? '').trim() || !(v.destinationAddress ?? '').trim()) {
      return 'Заполните пункт отправления и пункт назначения.';
    }
    if (v.loadDate == null || v.unloadDate == null) {
      return 'Укажите плановые даты погрузки и разгрузки.';
    }
    if (this.startOfDay(v.unloadDate) < this.startOfDay(v.loadDate)) {
      return 'Дата разгрузки не может быть раньше даты погрузки.';
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

  /** Создать черновик, записать TripUpdateRequest, отправить на согласование. */
  saveAndSubmit(): void {
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

    const body = this.buildUpdateRequest();
    this.busy = true;
    this.trips
      .create()
      .pipe(
        concatMap((trip) =>
          this.trips.update(trip.id, body).pipe(
            concatMap(() => this.trips.submit(trip.id)),
          ),
        ),
      )
      .subscribe({
        next: (t) => {
          this.busy = false;
          void this.router.navigate(['/trips', t.id]);
        },
        error: (err: HttpErrorResponse) => {
          this.handleError(err);
          this.busy = false;
        },
      });
  }
}
