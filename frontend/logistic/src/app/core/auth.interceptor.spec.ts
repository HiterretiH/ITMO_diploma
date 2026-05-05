import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('adds Authorization when token present and not login', () => {
    sessionStorage.setItem('access_token', 'abc');
    http.get('/api/v1/trips').subscribe();
    const req = httpMock.expectOne('/api/v1/trips');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
    req.flush([]);
  });

  it('does not add Authorization for login', () => {
    sessionStorage.setItem('access_token', 'abc');
    http.post('/api/v1/auth/login', {}).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does not add Authorization for register', () => {
    sessionStorage.setItem('access_token', 'abc');
    http.post('/api/v1/auth/register', {}).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/register');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
