import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AuditApiService } from '../../core/audit-api.service';
import { OrderApiService } from '../../core/order-api.service';
import { CatalogApiService } from '../../core/catalog-api.service';
import { OrderResponse } from '../../core/order.models';
import { TripEditComponent } from './trip-edit.component';

describe('TripEditComponent', () => {
  let fixture: ComponentFixture<TripEditComponent>;

  const order: OrderResponse = {
    id: 1,
    customerId: 1,
    performerId: 1,
    vehicleId: 1,
    driverId: 1,
    orderNumber: 1,
    orderDate: '2026-01-01',
    loadingPlace: 'A',
    loadingContact: null,
    unloadingPlace: 'B',
    unloadingContact: null,
    tripCount: 1,
    pricePerTrip: null,
    totalPrice: 100,
    templateVersion: 1,
    completed: false,
    customerShortName: 'Cust',
    performerShortName: 'Perf',
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
          provide: OrderApiService,
          useValue: {
            get: () => of(order),
            update: () => of(order),
            complete: () => of(order),
            delete: () => of(void 0),
            listDocuments: () => of([]),
            downloadDocument: () => of(new Blob()),
          },
        },
        {
          provide: AuditApiService,
          useValue: {
            listByOrder: () => of([]),
          },
        },
        {
          provide: CatalogApiService,
          useValue: {
            listCustomers: () => of([]),
            listPerformers: () => of([]),
            listDrivers: () => of([]),
            listVehicles: () => of([]),
            createCustomer: () => of({} as never),
            updateCustomer: () => of({} as never),
            createPerformer: () => of({} as never),
            updatePerformer: () => of({} as never),
            createDriver: () => of({} as never),
            updateDriver: () => of({} as never),
            createVehicle: () => of({} as never),
            updateVehicle: () => of({} as never),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TripEditComponent);
  });

  it('loads order and patches loading/unloading fields', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.order?.id).toBe(1);
    expect(cmp.form.controls.loadingPlace.value).toBe('A');
    expect(cmp.form.controls.unloadingPlace.value).toBe('B');
    expect(cmp.form.controls.orderNumber.value).toBe(1);
  });

  it('inProgress when audit has no ORDER_COMPLETED', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.inProgress()).toBe(true);
  });
});
