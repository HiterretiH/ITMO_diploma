import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { TripApiService } from '../../core/trip-api.service';
import {
  GeneratedDocumentResponse,
  TripResponse,
  TripUpdateRequest,
} from '../../core/trip.models';

@Component({
  selector: 'app-trip-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trip-edit.component.html',
  styleUrl: './trip-edit.component.css',
})
export class TripEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trips = inject(TripApiService);
  private readonly catalog = inject(CatalogApiService);
  readonly auth = inject(AuthService);

  trip: TripResponse | null = null;
  docs: GeneratedDocumentResponse[] = [];
  counterparties: CounterpartyResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  shipperId: number | null = null;
  consigneeId: number | null = null;
  driverId: number | null = null;
  vehicleId: number | null = null;
  cargoDescription = '';
  cargoWeightKg: number | null = null;
  routeFrom = '';
  routeTo = '';
  loadDate = '';
  unloadDate = '';
  priceAmount: number | null = null;
  currency = 'RUB';

  error: string | null = null;
  busy = false;

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('tripId');
    if (!raw) {
      void this.router.navigateByUrl('/trips');
      return;
    }
    if (raw === 'new') {
      this.busy = true;
      this.trips.create().subscribe({
        next: (t) => void this.router.navigate(['/trips', t.id], { replaceUrl: true }),
        error: () => {
          this.error = 'Не удалось создать рейс';
          this.busy = false;
        },
      });
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
    this.error = null;
    this.busy = true;
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
        this.patch(trip);
        if (trip.status === 'APPROVED' || trip.status === 'ARCHIVED') {
          this.trips.documents(id).subscribe({
            next: (d) => (this.docs = d),
            error: () => (this.docs = []),
          });
        } else {
          this.docs = [];
        }
        this.busy = false;
      },
      error: () => {
        this.error = 'Не удалось загрузить рейс';
        this.busy = false;
      },
    });
  }

  private patch(t: TripResponse): void {
    this.shipperId = t.shipperId;
    this.consigneeId = t.consigneeId;
    this.driverId = t.driverId;
    this.vehicleId = t.vehicleId;
    this.cargoDescription = t.cargoDescription ?? '';
    this.cargoWeightKg =
      t.cargoWeightKg === null || t.cargoWeightKg === undefined
        ? null
        : Number(t.cargoWeightKg);
    this.routeFrom = t.routeFrom ?? '';
    this.routeTo = t.routeTo ?? '';
    this.loadDate = t.loadDate ? t.loadDate.slice(0, 10) : '';
    this.unloadDate = t.unloadDate ? t.unloadDate.slice(0, 10) : '';
    this.priceAmount =
      t.priceAmount === null || t.priceAmount === undefined ? null : Number(t.priceAmount);
    this.currency = t.currency ?? 'RUB';
  }

  get tripId(): number | null {
    return this.trip?.id ?? null;
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

  save(): void {
    const id = this.tripId;
    if (!id) {
      return;
    }
    const body: TripUpdateRequest = {
      shipperId: this.shipperId,
      consigneeId: this.consigneeId,
      driverId: this.driverId,
      vehicleId: this.vehicleId,
      cargoDescription: this.cargoDescription || null,
      cargoWeightKg: this.cargoWeightKg,
      routeFrom: this.routeFrom || null,
      routeTo: this.routeTo || null,
      loadDate: this.loadDate || null,
      unloadDate: this.unloadDate || null,
      priceAmount: this.priceAmount,
      currency: this.currency || null,
    };
    this.busy = true;
    this.trips.update(id, body).subscribe({
      next: (t) => {
        this.trip = t;
        this.patch(t);
        this.busy = false;
      },
      error: () => {
        this.error = 'Сохранение не удалось';
        this.busy = false;
      },
    });
  }

  submit(): void {
    const id = this.tripId;
    if (!id) {
      return;
    }
    this.busy = true;
    this.trips.submit(id).subscribe({
      next: (t) => {
        this.trip = t;
        this.patch(t);
        this.busy = false;
      },
      error: () => {
        this.error = 'Отправка не удалась';
        this.busy = false;
      },
    });
  }

  approve(): void {
    const id = this.tripId;
    if (!id) {
      return;
    }
    this.busy = true;
    this.trips.approve(id).subscribe({
      next: (t) => {
        this.trip = t;
        this.patch(t);
        this.reload(id);
        this.busy = false;
      },
      error: () => {
        this.error = 'Согласование не удалось';
        this.busy = false;
      },
    });
  }

  archive(): void {
    const id = this.tripId;
    if (!id) {
      return;
    }
    this.busy = true;
    this.trips.archive(id).subscribe({
      next: (t) => {
        this.trip = t;
        this.patch(t);
        this.busy = false;
      },
      error: () => {
        this.error = 'Архивация не удалась';
        this.busy = false;
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

  statusLabel(): string {
    const s = this.trip?.status;
    if (!s) {
      return '';
    }
    switch (s) {
      case 'DRAFT':
        return 'Черновик';
      case 'PENDING_APPROVAL':
        return 'На согласовании';
      case 'APPROVED':
        return 'Утверждён';
      case 'ARCHIVED':
        return 'Архив';
      default:
        return s;
    }
  }
}
