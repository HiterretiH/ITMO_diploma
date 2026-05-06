import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CatalogApiService } from '../../core/catalog-api.service';
import { TripApiService } from '../../core/trip-api.service';
import { TripUpdateRequest } from '../../core/trip.models';
import { TripNewComponent } from './trip-new.component';

@Component({ standalone: true, template: '' })
class TripDetailRouteStub {}

describe('TripNewComponent', () => {
  let fixture: ComponentFixture<TripNewComponent>;
  let lastUpdatePayload: TripUpdateRequest | undefined;

  beforeEach(async () => {
    lastUpdatePayload = undefined;
    await TestBed.configureTestingModule({
      imports: [TripNewComponent, TripDetailRouteStub],
      providers: [
        provideNoopAnimations(),
        provideRouter([{ path: 'trips/:id', component: TripDetailRouteStub }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: TripApiService,
          useValue: {
            create: () => of({ id: 99 }),
            update: (_id: number, body: TripUpdateRequest) => {
              lastUpdatePayload = body;
              return of({ id: 99 });
            },
            complete: () => of({ id: 99 }),
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

  it('autofills consignee cargo unloadDate in update payload', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    const d = new Date(2026, 4, 15, 12, 0, 0, 0);
    cmp.form.patchValue({
      shipperId: 7,
      driverId: 1,
      vehicleId: 2,
      originAddress: 'A',
      destinationAddress: 'B',
      loadDate: d,
      legCount: 2,
      ratePerLeg: 100,
    });
    expect(cmp.form.controls.priceAmount.value).toBe(200);
    cmp.saveAndComplete();
    expect(lastUpdatePayload).toBeDefined();
    expect(lastUpdatePayload!.consigneeId).toBe(7);
    expect(lastUpdatePayload!.shipperId).toBe(7);
    expect(lastUpdatePayload!.cargoDescription).toBe('-');
    expect(lastUpdatePayload!.cargoWeightKg).toBe(0);
    expect(lastUpdatePayload!.loadDate).toBe('2026-05-15');
    expect(lastUpdatePayload!.unloadDate).toBe('2026-05-15');
    expect(lastUpdatePayload!.priceAmount).toBe(200);
  });
});
