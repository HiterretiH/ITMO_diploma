import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AuditApiService } from '../../core/audit-api.service';
import { TripApiService } from '../../core/trip-api.service';
import { CatalogApiService } from '../../core/catalog-api.service';
import { TripResponse } from '../../core/trip.models';
import { TripEditComponent } from './trip-edit.component';

describe('TripEditComponent', () => {
  let fixture: ComponentFixture<TripEditComponent>;

  const tripInProgress: TripResponse = {
    id: 1,
    ownerId: 1,
    ownerUsername: 'u',
    status: 'IN_PROGRESS',
    shipperId: null,
    consigneeId: null,
    driverId: null,
    vehicleId: null,
    cargoDescription: null,
    cargoWeightKg: null,
    routeFrom: 'Склад А\nКонтакт: +7999',
    routeTo: 'Точка Б',
    loadDate: null,
    unloadDate: null,
    priceAmount: null,
    currency: 'RUB',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripEditComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        MessageService,
        ConfirmationService,
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => '1' } },
          },
        },
        {
          provide: TripApiService,
          useValue: {
            get: () => of(tripInProgress),
            documents: () => of([]),
            update: () => of(tripInProgress),
            complete: () => of({ ...tripInProgress, status: 'COMPLETED' }),
            delete: () => of(void 0),
          },
        },
        {
          provide: AuditApiService,
          useValue: {
            listByTrip: () => of([]),
          },
        },
        {
          provide: CatalogApiService,
          useValue: {
            counterparties: () => of([]),
            drivers: () => of([]),
            vehicles: () => of([]),
            places: () => of([]),
            createPlace: () =>
              of({ id: 1, address: 'A', contact: null, placeType: 'LOAD' }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TripEditComponent);
  });

  it('enables form in IN_PROGRESS', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.inProgress()).toBe(true);
    expect(cmp.form.disabled).toBe(false);
  });

  it('unpacks routeFrom/routeTo into address and contact fields', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.form.get('originAddress')?.value).toBe('Склад А');
    expect(cmp.form.get('originContact')?.value).toBe('+7999');
    expect(cmp.form.get('destinationAddress')?.value).toBe('Точка Б');
    expect(cmp.form.get('destinationContact')?.value).toBe('');
  });

  it('keeps form enabled in COMPLETED', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.trip = {
      ...tripInProgress,
      status: 'COMPLETED',
    };
    (cmp as unknown as { patchForm: (t: TripResponse) => void }).patchForm(
      cmp.trip,
    );
    (cmp as unknown as { syncFormDisabled: () => void }).syncFormDisabled();
    expect(cmp.completed()).toBe(true);
    expect(cmp.form.disabled).toBe(false);
  });
});
