import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CatalogApiService } from '../../core/catalog-api.service';
import { OrderApiService } from '../../core/order-api.service';
import { OrderCreateRequest } from '../../core/order.models';
import {
  orderPrintDemoCatalogIds,
  orderPrintDemoTripFormPatch,
} from '../../fixtures/order-print-demo.fixture';
import { TripNewComponent } from './trip-new.component';

@Component({ standalone: true, template: '' })
class OrderDetailRouteStub {}

describe('TripNewComponent', () => {
  let fixture: ComponentFixture<TripNewComponent>;
  let lastCreatePayload: OrderCreateRequest | undefined;

  beforeEach(async () => {
    lastCreatePayload = undefined;
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
            create: (body: OrderCreateRequest) => {
              lastCreatePayload = body;
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
            getCustomerRouteHints: () => of([]),
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

  it('sends OrderCreateRequest with route and totals', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.form.patchValue({
      ...orderPrintDemoCatalogIds,
      ...orderPrintDemoTripFormPatch,
    });
    expect(cmp.form.controls.priceAmount.value).toBe(98500);
    cmp.saveDraft();
    expect(lastCreatePayload).toBeDefined();
    expect(lastCreatePayload!.tripCount).toBe(1);
    expect(lastCreatePayload!.totalPrice).toBe(98500);
    expect(lastCreatePayload!.pricePerTrip).toBe(98500);
    expect(lastCreatePayload!.loadingPlace).toBe('Москва');
    expect(lastCreatePayload!.unloadingPlace).toBe('Санкт-Петербург');
    expect(lastCreatePayload!.loadingContact).toBe(
      'Контакт погрузки +7 900 123 45 67',
    );
    expect(lastCreatePayload!.unloadingContact).toBe(
      'Контакт разгрузки +7 900 765 43 21',
    );
    expect(lastCreatePayload!.orderDate).toBe('2026-05-15');
    expect(lastCreatePayload!.customerId).toBe(1);
    expect(lastCreatePayload!.performerId).toBe(2);
  });

  it('includes orderNumber in create when set', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.form.patchValue({
      ...orderPrintDemoCatalogIds,
      ...orderPrintDemoTripFormPatch,
    });
    cmp.saveDraft();
    expect(lastCreatePayload?.orderNumber).toBe(42);
  });
});
