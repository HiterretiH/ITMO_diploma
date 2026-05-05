import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CatalogApiService } from '../../core/catalog-api.service';
import { TripApiService } from '../../core/trip-api.service';
import { TripNewComponent } from './trip-new.component';

describe('TripNewComponent', () => {
  let fixture: ComponentFixture<TripNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripNewComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: TripApiService,
          useValue: {
            create: () => of({ id: 1 }),
            update: () => of({ id: 1 }),
            submit: () => of({ id: 1 }),
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

  it('moves unloadDate forward when earlier than loadDate', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    const load = new Date(2026, 0, 10, 12, 0, 0, 0);
    const unloadEarly = new Date(2026, 0, 5, 12, 0, 0, 0);
    cmp.form.patchValue({
      loadDate: load,
      unloadDate: unloadEarly,
    });
    cmp.form.controls.loadDate.updateValueAndValidity();
    cmp.form.controls.loadDate.setValue(new Date(load.getTime()));
    expect(cmp.form.controls.unloadDate.value?.getDate()).toBe(10);
  });
});
