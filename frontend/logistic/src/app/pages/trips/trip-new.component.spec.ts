import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CatalogApiService } from '../../core/catalog-api.service';
import { OrderApiService } from '../../core/order-api.service';
import { OrderUpdateRequest } from '../../core/order.models';
import { TripNewComponent } from './trip-new.component';

@Component({ standalone: true, template: '' })
class OrderDetailRouteStub {}

describe('TripNewComponent', () => {
  let fixture: ComponentFixture<TripNewComponent>;
  let lastUpdatePayload: OrderUpdateRequest | undefined;

  beforeEach(async () => {
    lastUpdatePayload = undefined;
    await TestBed.configureTestingModule({
      imports: [TripNewComponent, OrderDetailRouteStub],
      providers: [
        provideNoopAnimations(),
        provideRouter([{ path: 'orders/:id', component: OrderDetailRouteStub }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: OrderApiService,
          useValue: {
            getTripFormDraft: () =>
              of({
                nextOrderNumber: 1,
                lastPerformerId: null,
                lastDriverId: null,
                lastVehicleId: null,
              }),
            create: () =>
              of({
                id: 99,
                customerId: 1,
                performerId: 1,
                vehicleId: 1,
                driverId: 1,
                orderNumber: 1,
                orderDate: '2026-05-15',
                loadingPlace: 'A',
                loadingContact: null,
                unloadingPlace: 'B',
                unloadingContact: null,
                tripCount: 2,
                pricePerTrip: 100,
                totalPrice: 200,
                templateVersion: 1,
                completed: false,
                customerShortName: 'C',
                performerShortName: 'P',
              }),
            update: (_id: number, body: OrderUpdateRequest) => {
              lastUpdatePayload = body;
              return of({
                id: 99,
                customerId: 1,
                performerId: 1,
                vehicleId: 1,
                driverId: 1,
                orderNumber: 1,
                orderDate: '2026-05-15',
                loadingPlace: 'A',
                loadingContact: null,
                unloadingPlace: 'B',
                unloadingContact: null,
                tripCount: 2,
                pricePerTrip: 100,
                totalPrice: 200,
                templateVersion: 1,
                completed: false,
                customerShortName: 'C',
                performerShortName: 'P',
              });
            },
          },
        },
        {
          provide: CatalogApiService,
          useValue: {
            listCustomers: () => of([]),
            listPerformers: () => of([]),
            listDrivers: () => of([]),
            listVehicles: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TripNewComponent);
  });

  it('recalculates priceAmount when ratePerLeg changes', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.form.patchValue({ legCount: 4, ratePerLeg: 25 });
    expect(cmp.form.controls.priceAmount.value).toBe(100);
  });

  it('recalculates priceAmount when legCount changes', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.form.patchValue({ ratePerLeg: 50, legCount: 1 });
    cmp.form.controls.legCount.setValue(3);
    expect(cmp.form.controls.priceAmount.value).toBe(150);
  });

  it('overwrites manual priceAmount when ratePerLeg changes again', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.form.patchValue({ ratePerLeg: 10, legCount: 2 });
    expect(cmp.form.controls.priceAmount.value).toBe(20);
    cmp.form.controls.priceAmount.setValue(999);
    expect(cmp.form.controls.priceAmount.value).toBe(999);
    cmp.form.controls.ratePerLeg.setValue(5);
    expect(cmp.form.controls.priceAmount.value).toBe(10);
  });

  it('sends OrderUpdateRequest with route and totals', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    const d = new Date(2026, 4, 15, 12, 0, 0, 0);
    cmp.form.patchValue({
      customerId: 1,
      performerId: 2,
      driverId: 1,
      vehicleId: 2,
      loadingPlace: 'A',
      unloadingPlace: 'B',
      orderDate: d,
      legCount: 2,
      ratePerLeg: 100,
    });
    expect(cmp.form.controls.priceAmount.value).toBe(200);
    cmp.saveDraft();
    expect(lastUpdatePayload).toBeDefined();
    expect(lastUpdatePayload!.tripCount).toBe(2);
    expect(lastUpdatePayload!.totalPrice).toBe(200);
    expect(lastUpdatePayload!.loadingPlace).toBe('A');
    expect(lastUpdatePayload!.orderDate).toBe('2026-05-15');
  });

  it('includes orderNumber in update when set', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    const d = new Date(2026, 4, 15, 12, 0, 0, 0);
    cmp.form.patchValue({
      customerId: 1,
      performerId: 2,
      driverId: 1,
      vehicleId: 2,
      loadingPlace: 'A',
      unloadingPlace: 'B',
      orderDate: d,
      legCount: 2,
      ratePerLeg: 100,
      orderNumber: 42,
    });
    cmp.saveDraft();
    expect(lastUpdatePayload?.orderNumber).toBe(42);
  });
});
