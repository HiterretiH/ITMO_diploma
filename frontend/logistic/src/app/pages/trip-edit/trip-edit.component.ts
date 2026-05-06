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
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { AuditApiService } from '../../core/audit-api.service';
import { AuditEventResponse } from '../../core/audit.models';
import { localizeProblemToast } from '../../core/error-messages';
import { TripApiService } from '../../core/trip-api.service';
import {
  GeneratedDocumentResponse,
  TripResponse,
  TripUpdateRequest,
} from '../../core/trip.models';
import { ProblemDetail } from '../../models/problem.models';
import { TripStatusBadgeComponent } from '../../shared/layout/trip-status-badge.component';
import { innValidator } from '../../shared/forms/inn.validator';
import { PlaceInputComponent } from '../../shared/forms/place-input.component';
import { buildRouteLine, parseRouteLine } from '../../shared/forms/route-line.util';
import { plateValidator } from '../../shared/forms/plate.validator';

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
    TripStatusBadgeComponent,
    PlaceInputComponent,
  ],
  templateUrl: './trip-edit.component.html',
  styleUrl: './trip-edit.component.css',
})
export class TripEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trips = inject(TripApiService);
  private readonly auditApi = inject(AuditApiService);
  private readonly catalog = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  trip: TripResponse | null = null;
  auditEvents: AuditEventResponse[] = [];
  mainTab: string | number = 'edit';
  docs: GeneratedDocumentResponse[] = [];
  counterparties: CounterpartyResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  busy = false;
  stepperValue: number | undefined = 1;
  conflictDetail: string | null = null;

  cpDialogVisible = false;
  cpTarget: 'shipper' | 'consignee' = 'shipper';
  cpEditingId: number | null = null;
  cpSaving = false;
  readonly cpForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(1)]],
    inn: ['', [innValidator]],
    legalAddress: [''],
    phone: [''],
  });

  driverDialogVisible = false;
  driverEditingId: number | null = null;
  driverSaving = false;
  readonly driverForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(1)]],
    licenseNumber: ['', [Validators.required, Validators.minLength(1)]],
    licenseCategory: [''],
  });

  vehicleDialogVisible = false;
  vehicleEditingId: number | null = null;
  vehicleSaving = false;
  readonly vehicleForm = this.fb.group({
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    model: [''],
    loadCapacityKg: this.fb.control<number | null>(null, {
      validators: [Validators.min(0)],
    }),
  });

  readonly form = this.fb.group({
    shipperId: this.fb.control<number | null>(null),
    consigneeId: this.fb.control<number | null>(null),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
    cargoDescription: [''],
    cargoWeightKg: this.fb.control<number | null>(null, [Validators.min(0)]),
    originAddress: [''],
    originContact: [''],
    destinationAddress: [''],
    destinationContact: [''],
    loadDate: this.fb.control<Date | null>(null),
    unloadDate: this.fb.control<Date | null>(null),
    priceAmount: this.fb.control<number | null>(null, [Validators.min(0)]),
    currency: ['RUB'],
  });

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('tripId');
    if (!raw) {
      void this.router.navigateByUrl('/trips');
      return;
    }
    const id = Number(raw);
    if (!Number.isFinite(id)) {
      void this.router.navigateByUrl('/trips');
      return;
    }
    this.reload(id);
  }

  reload(id: number): void {
    this.busy = true;
    this.conflictDetail = null;
    forkJoin({
      trip: this.trips.get(id),
      cp: this.catalog.counterparties(),
      dr: this.catalog.drivers(),
      ve: this.catalog.vehicles(),
    }).subscribe({
      next: ({ trip, cp, dr, ve }) => {
        this.trip = trip;
        this.counterparties = cp;
        this.drivers = dr;
        this.vehicles = ve;
        this.patchForm(trip);
        this.syncFormDisabled();
        if (trip.status === 'COMPLETED') {
          this.trips.documents(id).subscribe({
            next: (d) => (this.docs = d),
            error: () => (this.docs = []),
          });
        } else {
          this.docs = [];
        }
        this.auditApi.listByTrip(id).subscribe({
          next: (ev) => (this.auditEvents = ev),
          error: () => (this.auditEvents = []),
        });
        this.busy = false;
      },
      error: () => {
        this.busy = false;
        void this.router.navigateByUrl('/trips');
      },
    });
  }

  private patchForm(t: TripResponse): void {
    const toDate = (s: string | null): Date | null =>
      s ? new Date(s.slice(0, 10) + 'T12:00:00') : null;
    this.form.patchValue({
      shipperId: t.shipperId,
      consigneeId: t.consigneeId,
      driverId: t.driverId,
      vehicleId: t.vehicleId,
      cargoDescription: t.cargoDescription ?? '',
      cargoWeightKg:
        t.cargoWeightKg === null || t.cargoWeightKg === undefined
          ? null
          : Number(t.cargoWeightKg),
      ...(() => {
        const from = parseRouteLine(t.routeFrom);
        const to = parseRouteLine(t.routeTo);
        return {
          originAddress: from.address,
          originContact: from.contact,
          destinationAddress: to.address,
          destinationContact: to.contact,
        };
      })(),
      loadDate: toDate(t.loadDate),
      unloadDate: toDate(t.unloadDate),
      priceAmount:
        t.priceAmount === null || t.priceAmount === undefined
          ? null
          : Number(t.priceAmount),
      currency: t.currency ?? 'RUB',
    });
  }

  private syncFormDisabled(): void {
    this.form.enable({ emitEvent: false });
  }

  inProgress(): boolean {
    return this.trip?.status === 'IN_PROGRESS';
  }

  completed(): boolean {
    return this.trip?.status === 'COMPLETED';
  }

  /** Правка справочников и полей — и в работе, и после завершения. */
  canEditCatalog(): boolean {
    return this.inProgress() || this.completed();
  }

  showDocuments(): boolean {
    return this.completed();
  }

  get tripId(): number | null {
    return this.trip?.id ?? null;
  }

  cpOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.counterparties.map((c) => ({ label: c.name, value: c.id })),
    ];
  }

  driverOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.drivers.map((d) => ({
        label: d.fullName,
        value: d.id,
      })),
    ];
  }

  vehicleOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.vehicles.map((v) => ({
        label: v.plateNumber,
        value: v.id,
      })),
    ];
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

  private buildBody(): TripUpdateRequest {
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
      priceAmount: v.priceAmount,
      currency: (v.currency ?? '').trim() || null,
    };
  }

  /** Совпадает с TripService.validateReadyForComplete (сообщение для UI). */
  private incompleteTripHint(): string | null {
    const v = this.form.getRawValue();
    if (
      v.shipperId == null ||
      v.consigneeId == null ||
      v.driverId == null ||
      v.vehicleId == null
    ) {
      return 'Выберите отправителя, получателя, водителя и транспорт.';
    }
    if (!(v.cargoDescription ?? '').trim()) {
      return 'Укажите описание груза.';
    }
    if (v.cargoWeightKg == null) {
      return 'Укажите вес груза.';
    }
    if (
      !(v.originAddress ?? '').trim() ||
      !(v.destinationAddress ?? '').trim()
    ) {
      return 'Заполните маршрут (откуда и куда).';
    }
    if (v.loadDate == null || v.unloadDate == null) {
      return 'Укажите даты погрузки и разгрузки.';
    }
    if (v.priceAmount == null) {
      return 'Укажите сумму.';
    }
    return null;
  }

  private applyTripResponse(t: TripResponse): void {
    this.trip = t;
    this.patchForm(t);
    this.syncFormDisabled();
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
      this.conflictDetail =
        detail || summary || 'Конфликт при сохранении';
      const errors = p.errors;
      if (Array.isArray(errors)) {
        for (const e of errors) {
          const f = e.field;
          if (f && this.form.get(f)) {
            this.form.get(f)?.setErrors({ server: true });
          }
        }
      }
      return;
    }
    this.conflictDetail = null;
  }

  save(): void {
    const id = this.tripId;
    if (!id || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy = true;
    this.conflictDetail = null;
    this.trips.update(id, this.buildBody()).subscribe({
      next: (t) => {
        this.applyTripResponse(t);
        if (t.status === 'COMPLETED' && id != null) {
          this.trips.documents(id).subscribe({
            next: (d) => (this.docs = d),
            error: () => (this.docs = []),
          });
        }
        this.busy = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleSaveError(err);
        this.busy = false;
      },
    });
  }

  complete(): void {
    const id = this.tripId;
    if (!id || !this.inProgress()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const hint = this.incompleteTripHint();
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
        this.trips
          .update(id, this.buildBody())
          .pipe(concatMap(() => this.trips.complete(id)))
          .subscribe({
            next: (t) => {
              this.applyTripResponse(t);
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

  deleteTrip(): void {
    const id = this.tripId;
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
        this.trips.delete(id).subscribe({
          next: () => {
            this.busy = false;
            void this.router.navigate(['/trips']);
          },
          error: () => {
            this.busy = false;
          },
        });
      },
    });
  }

  download(doc: GeneratedDocumentResponse): void {
    this.trips.downloadFile(doc.id).subscribe({
      next: (blob) => {
        const ext = doc.fileFormat === 'PDF' ? 'pdf' : 'docx';
        const name = `${doc.documentType.toLowerCase()}.${ext}`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  cpDialogTitle(): string {
    return this.cpEditingId != null ? 'Изменить контрагента' : 'Новый контрагент';
  }

  cpSaveLabel(): string {
    return this.cpEditingId != null ? 'Сохранить' : 'Создать';
  }

  shipperSelected(): boolean {
    return this.form.getRawValue().shipperId != null;
  }

  consigneeSelected(): boolean {
    return this.form.getRawValue().consigneeId != null;
  }

  driverSelected(): boolean {
    return this.form.getRawValue().driverId != null;
  }

  vehicleSelected(): boolean {
    return this.form.getRawValue().vehicleId != null;
  }

  openCpDialog(target: 'shipper' | 'consignee'): void {
    if (!this.canEditCatalog()) {
      return;
    }
    this.cpTarget = target;
    this.cpEditingId = null;
    this.cpForm.reset({
      name: '',
      inn: '',
      legalAddress: '',
      phone: '',
    });
    this.cpDialogVisible = true;
  }

  openCpEdit(target: 'shipper' | 'consignee'): void {
    if (!this.canEditCatalog()) {
      return;
    }
    this.cpTarget = target;
    const id =
      target === 'shipper'
        ? this.form.getRawValue().shipperId
        : this.form.getRawValue().consigneeId;
    if (id == null) {
      return;
    }
    const c = this.counterparties.find((x) => x.id === id);
    if (!c) {
      return;
    }
    this.cpEditingId = c.id;
    this.cpForm.setValue({
      name: c.name,
      inn: c.inn ?? '',
      legalAddress: c.legalAddress ?? '',
      phone: c.phone ?? '',
    });
    this.cpDialogVisible = true;
  }

  openDriverCreate(): void {
    if (!this.canEditCatalog()) {
      return;
    }
    this.driverEditingId = null;
    this.driverForm.reset({
      fullName: '',
      licenseNumber: '',
      licenseCategory: '',
    });
    this.driverDialogVisible = true;
  }

  openDriverEdit(): void {
    if (!this.canEditCatalog()) {
      return;
    }
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
      licenseNumber: d.licenseNumber,
      licenseCategory: d.licenseCategory ?? '',
    });
    this.driverDialogVisible = true;
  }

  openVehicleCreate(): void {
    if (!this.canEditCatalog()) {
      return;
    }
    this.vehicleEditingId = null;
    this.vehicleForm.reset({
      plateNumber: '',
      model: '',
      loadCapacityKg: null,
    });
    this.vehicleDialogVisible = true;
  }

  openVehicleEdit(): void {
    if (!this.canEditCatalog()) {
      return;
    }
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
      plateNumber: v.plateNumber,
      model: v.model ?? '',
      loadCapacityKg: v.loadCapacityKg,
    });
    this.vehicleDialogVisible = true;
  }

  driverDialogTitle(): string {
    return this.driverEditingId != null ? 'Изменить водителя' : 'Новый водитель';
  }

  vehicleDialogTitle(): string {
    return this.vehicleEditingId != null ? 'Изменить ТС' : 'Новое ТС';
  }

  driverSaveLabel(): string {
    return this.driverEditingId != null ? 'Сохранить' : 'Создать';
  }

  vehicleSaveLabel(): string {
    return this.vehicleEditingId != null ? 'Сохранить' : 'Создать';
  }

  saveDriverDialog(): void {
    if (this.driverForm.invalid || this.driverSaving) {
      this.driverForm.markAllAsTouched();
      return;
    }
    const v = this.driverForm.getRawValue();
    const body = {
      fullName: v.fullName.trim(),
      licenseNumber: v.licenseNumber.trim(),
      licenseCategory:
        v.licenseCategory.trim() === '' ? null : v.licenseCategory.trim(),
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
    if (this.vehicleForm.invalid || this.vehicleSaving) {
      this.vehicleForm.markAllAsTouched();
      return;
    }
    const v = this.vehicleForm.getRawValue();
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const modelTrim = (v.model ?? '').trim();
    const body = {
      plateNumber: plate,
      model: modelTrim === '' ? null : modelTrim,
      loadCapacityKg:
        v.loadCapacityKg === null || v.loadCapacityKg === undefined
          ? null
          : Math.floor(Number(v.loadCapacityKg)),
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
            .sort((a, b) => a.plateNumber.localeCompare(b.plateNumber));
        } else {
          this.vehicles = [...this.vehicles, ve].sort((a, b) =>
            a.plateNumber.localeCompare(b.plateNumber),
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
      case 'TRIP_CREATED':
        return 'Создание рейса';
      case 'TRIP_UPDATED':
        return 'Изменение';
      case 'TRIP_COMPLETED':
        return 'Завершён';
      case 'TRIP_DELETED':
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

  saveCpDialog(): void {
    if (this.cpForm.invalid || this.cpSaving) {
      this.cpForm.markAllAsTouched();
      return;
    }
    const v = this.cpForm.getRawValue();
    const body = {
      name: v.name.trim(),
      inn: v.inn.trim() === '' ? null : v.inn.trim(),
      legalAddress:
        v.legalAddress.trim() === '' ? null : v.legalAddress.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
    };
    this.cpSaving = true;
    const obs =
      this.cpEditingId != null
        ? this.catalog.updateCounterparty(this.cpEditingId, body)
        : this.catalog.createCounterparty(body);
    obs.subscribe({
      next: (c) => {
        if (this.cpEditingId != null) {
          this.counterparties = this.counterparties
            .map((x) => (x.id === c.id ? c : x))
            .sort((a, b) => a.name.localeCompare(b.name));
        } else {
          this.counterparties = [...this.counterparties, c].sort((a, b) =>
            a.name.localeCompare(b.name),
          );
          if (this.cpTarget === 'shipper') {
            this.form.patchValue({ shipperId: c.id });
          } else {
            this.form.patchValue({ consigneeId: c.id });
          }
        }
        this.cpSaving = false;
        this.cpDialogVisible = false;
      },
      error: () => {
        this.cpSaving = false;
      },
    });
  }
}
