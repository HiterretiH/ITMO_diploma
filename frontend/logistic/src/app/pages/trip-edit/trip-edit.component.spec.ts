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
import { TripEditComponent } from './trip-edit.component';

describe('TripEditComponent', () => {
  let fixture: ComponentFixture<TripEditComponent>;

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
            get: () =>
              of({
                id: 1,
                ownerId: 1,
                ownerUsername: 'u',
                status: 'DRAFT',
                shipperId: null,
                consigneeId: null,
                driverId: null,
                vehicleId: null,
                cargoDescription: null,
                cargoWeightKg: null,
                routeFrom: null,
                routeTo: null,
                loadDate: null,
                unloadDate: null,
                priceAmount: null,
                currency: 'RUB',
                updatedAt: '2026-01-01T00:00:00Z',
              }),
            documents: () => of([]),
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
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TripEditComponent);
  });

  it('enables form in DRAFT', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.draft()).toBe(true);
    expect(cmp.form.disabled).toBe(false);
  });

  it('disables save when not DRAFT', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.trip = {
      id: 1,
      ownerId: 1,
      ownerUsername: 'u',
      status: 'PENDING_APPROVAL',
      shipperId: null,
      consigneeId: null,
      driverId: null,
      vehicleId: null,
      cargoDescription: null,
      cargoWeightKg: null,
      routeFrom: null,
      routeTo: null,
      loadDate: null,
      unloadDate: null,
      priceAmount: null,
      currency: 'RUB',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    (cmp as unknown as { patchForm: (t: typeof cmp.trip) => void }).patchForm(
      cmp.trip,
    );
    (cmp as unknown as { syncFormDisabled: () => void }).syncFormDisabled();
    expect(cmp.form.disabled).toBe(true);
  });
});
