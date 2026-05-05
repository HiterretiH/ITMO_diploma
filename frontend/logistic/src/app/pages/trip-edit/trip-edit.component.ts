import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
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
import { AuthService } from '../../core/auth.service';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { AuditApiService } from '../../core/audit-api.service';
import { AuditEventResponse } from '../../core/audit.models';
import { TripApiService } from '../../core/trip-api.service';
import {
  GeneratedDocumentResponse,
  TripResponse,
  TripUpdateRequest,
} from '../../core/trip.models';
import { ProblemDetail } from '../../models/problem.models';
import { TripStatusBadgeComponent } from '../../shared/layout/trip-status-badge.component';
import { innValidator } from '../../shared/forms/inn.validator';

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

  readonly auth = inject(AuthService);

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
  cpSaving = false;
  readonly quickCpForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(1)]],
    inn: ['', [innValidator]],
  });

  readonly form = this.fb.group({
    shipperId: this.fb.control<number | null>(null),
    consigneeId: this.fb.control<number | null>(null),
    driverId: this.fb.control<number | null>(null),
    vehicleId: this.fb.control<number | null>(null),
    cargoDescription: [''],
    cargoWeightKg: this.fb.control<number | null>(null, [Validators.min(0)]),
    routeFrom: [''],
    routeTo: [''],
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
        if (trip.status === 'APPROVED' || trip.status === 'ARCHIVED') {
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
      routeFrom: t.routeFrom ?? '',
      routeTo: t.routeTo ?? '',
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
    if (this.draft()) {
      this.form.enable({ emitEvent: false });
    } else {
      this.form.disable({ emitEvent: false });
    }
  }

  draft(): boolean {
    return this.trip?.status === 'DRAFT';
  }

  pending(): boolean {
    return this.trip?.status === 'PENDING_APPROVAL';
  }

  approved(): boolean {
    return this.trip?.status === 'APPROVED';
  }

  showDocuments(): boolean {
    return this.approved() || this.trip?.status === 'ARCHIVED';
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
      routeFrom: (v.routeFrom ?? '').trim() || null,
      routeTo: (v.routeTo ?? '').trim() || null,
      loadDate: this.toIsoDate(v.loadDate),
      unloadDate: this.toIsoDate(v.unloadDate),
      priceAmount: v.priceAmount,
      currency: (v.currency ?? '').trim() || null,
    };
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
      this.conflictDetail = p.detail || p.title || 'Конфликт при сохранении';
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
    if (!id || !this.draft() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy = true;
    this.conflictDetail = null;
    this.trips.update(id, this.buildBody()).subscribe({
      next: (t) => {
        this.applyTripResponse(t);
        this.busy = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleSaveError(err);
        this.busy = false;
      },
    });
  }

  submit(): void {
    const id = this.tripId;
    if (!id || !this.draft()) {
      return;
    }
    this.busy = true;
    this.trips.submit(id).subscribe({
      next: (t) => {
        this.applyTripResponse(t);
        this.busy = false;
      },
      error: () => {
        this.busy = false;
      },
    });
  }

  approve(): void {
    const id = this.tripId;
    if (!id || !this.pending()) {
      return;
    }
    this.confirm.confirm({
      message: 'Утвердить этот рейс?',
      header: 'Подтверждение',
      icon: 'pi pi-check-circle',
      accept: () => {
        this.busy = true;
        this.trips.approve(id).subscribe({
          next: (t) => {
            this.applyTripResponse(t);
            this.reload(id);
            this.busy = false;
          },
          error: () => {
            this.busy = false;
          },
        });
      },
    });
  }

  archive(): void {
    const id = this.tripId;
    if (!id || !this.approved()) {
      return;
    }
    this.confirm.confirm({
      message: 'Перевести рейс в архив?',
      header: 'Подтверждение',
      icon: 'pi pi-inbox',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.busy = true;
        this.trips.archive(id).subscribe({
          next: (t) => {
            this.applyTripResponse(t);
            this.reload(id);
            this.busy = false;
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

  openCpDialog(target: 'shipper' | 'consignee'): void {
    this.cpTarget = target;
    this.quickCpForm.reset({ name: '', inn: '' });
    this.cpDialogVisible = true;
  }

  eventTypeLabel(t: AuditEventResponse['eventType']): string {
    switch (t) {
      case 'TRIP_CREATED':
        return 'Создание рейса';
      case 'TRIP_UPDATED':
        return 'Изменение';
      case 'TRIP_SUBMITTED':
        return 'Отправлен на согласование';
      case 'TRIP_APPROVED':
        return 'Утверждён';
      case 'TRIP_ARCHIVED':
        return 'Архивирован';
      case 'DOCUMENTS_GENERATED':
        return 'Документы сформированы';
      case 'LOGIN':
        return 'Вход';
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

  saveQuickCounterparty(): void {
    if (this.quickCpForm.invalid || this.cpSaving) {
      this.quickCpForm.markAllAsTouched();
      return;
    }
    const v = this.quickCpForm.getRawValue();
    const body = {
      name: v.name.trim(),
      inn: v.inn.trim() === '' ? null : v.inn.trim(),
      legalAddress: null,
      phone: null,
    };
    this.cpSaving = true;
    this.catalog.createCounterparty(body).subscribe({
      next: (c) => {
        this.counterparties = [...this.counterparties, c].sort((a, b) =>
          a.name.localeCompare(b.name),
        );
        if (this.cpTarget === 'shipper') {
          this.form.patchValue({ shipperId: c.id });
        } else {
          this.form.patchValue({ consigneeId: c.id });
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
