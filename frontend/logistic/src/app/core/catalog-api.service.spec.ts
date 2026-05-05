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
  });

  it('places() sends type and q query params', () => {
    service.places('LOAD', 'склад').subscribe();

    const req = http.expectOne(
      (r) =>
        r.url.endsWith('/api/v1/places') &&
        r.params.get('type') === 'LOAD' &&
        r.params.get('q') === 'склад',
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createPlace posts body', () => {
    service
      .createPlace({
        address: 'А',
        contact: 'Б',
        placeType: 'BOTH',
      })
      .subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/api/v1/places'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      address: 'А',
      contact: 'Б',
      placeType: 'BOTH',
    });
    req.flush({ id: 1, address: 'А', contact: 'Б', placeType: 'BOTH' });
  });
});
