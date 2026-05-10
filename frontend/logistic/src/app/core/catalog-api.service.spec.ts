import {
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogApiService } from './catalog-api.service';

describe('CatalogApiService', () => {
  let service: CatalogApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        CatalogApiService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(CatalogApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('listCustomers sends q query param', () => {
    service.listCustomers('ООО').subscribe();

    const req = http.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url.startsWith('/api/v1/customers') &&
        r.url.includes(`q=${encodeURIComponent('ООО')}`),
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createCustomer posts body', () => {
    service
      .createCustomer({
        shortName: 'ООО Рога',
        fullName: 'Полное название',
      })
      .subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/api/v1/customers'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      shortName: 'ООО Рога',
      fullName: 'Полное название',
    });
    req.flush({
      id: 1,
      shortName: 'ООО Рога',
      fullName: 'Полное название',
      phone: null,
      requisites: null,
    });
  });
});
