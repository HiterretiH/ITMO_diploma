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
            complete: () => of(order),
            delete: () => of(void 0),
            listDocuments: () => of([]),
            downloadDocument: () =>
              of({ blob: new Blob(), fileName: 'f.docx' }),
          },
        },
        {
          provide: AuditApiService,
          useValue: {
            listByOrder: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TripEditComponent);
  });

  it('loads order for overview', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.order?.id).toBe(1);
    expect(cmp.routeSnippet(order)).toContain('A');
  });

  it('inProgress when order not completed', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.inProgress()).toBe(true);
  });
});
