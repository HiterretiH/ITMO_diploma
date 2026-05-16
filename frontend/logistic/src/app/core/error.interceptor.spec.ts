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
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { errorInterceptor } from './error.interceptor';
import { authInterceptor } from './auth.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let messages: MessageService;
  let authLogoutSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    const router = {
      navigateByUrl: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        MessageService,
        { provide: Router, useValue: router },
        provideHttpClient(
          withInterceptors([authInterceptor, errorInterceptor]),
        ),
        provideHttpClientTesting(),
      ],
    });
    authLogoutSpy = vi
      .spyOn(TestBed.inject(AuthService), 'logout')
      .mockImplementation(() => {});
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    messages = TestBed.inject(MessageService);
    vi.spyOn(messages, 'add').mockImplementation(() => {});
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('shows toast on login 401 with problem+json', () => {
    http.post('/api/v1/auth/login', {}).subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush(
      { title: 'Unauthorized', detail: 'Bad credentials', status: 401 },
      {
        status: 401,
        statusText: 'Unauthorized',
        headers: { 'Content-Type': 'application/problem+json' },
      },
    );
    expect(authLogoutSpy).not.toHaveBeenCalled();
    expect(messages.add).toHaveBeenCalled();
  });

  it('logs out on 401 for protected resource', () => {
    sessionStorage.setItem('access_token', 't');
    http.get('/api/v1/orders').subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/orders');
    req.flush('{}', {
      status: 401,
      statusText: 'Unauthorized',
      headers: { 'Content-Type': 'application/json' },
    });
    expect(authLogoutSpy).toHaveBeenCalled();
  });

  it('does not log out on 503 for protected resource', () => {
    sessionStorage.setItem('access_token', 't');
    http.get('/api/v1/orders').subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/orders');
    req.flush(
      { title: 'Service Unavailable', detail: 'Сервис временно недоступен', status: 503 },
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/problem+json' },
      },
    );
    expect(authLogoutSpy).not.toHaveBeenCalled();
    expect(messages.add).toHaveBeenCalled();
  });

  it('does not toast on 409 for order PUT', () => {
    sessionStorage.setItem('access_token', 't');
    http.put('/api/v1/orders/1', {}).subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/orders/1');
    req.flush(
      { title: 'Conflict', detail: 'x', status: 409 },
      {
        status: 409,
        statusText: 'Conflict',
        headers: { 'Content-Type': 'application/problem+json' },
      },
    );
    expect(messages.add).not.toHaveBeenCalled();
  });
});
