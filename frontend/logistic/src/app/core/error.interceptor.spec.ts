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
import { AuthService } from './auth.service';
import { errorInterceptor } from './error.interceptor';
import { authInterceptor } from './auth.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let messages: MessageService;
  let authLogoutSpy: jasmine.Spy;

  beforeEach(() => {
    const router = {
      navigateByUrl: jasmine.createSpy('navigateByUrl'),
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
    authLogoutSpy = spyOn(TestBed.inject(AuthService), 'logout');
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    messages = TestBed.inject(MessageService);
    spyOn(messages, 'add');
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
    http.get('/api/v1/trips').subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/trips');
    req.flush('{}', {
      status: 401,
      statusText: 'Unauthorized',
      headers: { 'Content-Type': 'application/json' },
    });
    expect(authLogoutSpy).toHaveBeenCalled();
  });

  it('does not toast on 409 for trip PATCH', () => {
    sessionStorage.setItem('access_token', 't');
    http.patch('/api/v1/trips/1', {}).subscribe({
      error: () => {
        /* expected */
      },
    });
    const req = httpMock.expectOne('/api/v1/trips/1');
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
