import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    sessionStorage.clear();
  });

  it('extracts roles from stored JWT payload', () => {
    const payloadJson = JSON.stringify({ roles: ['MANAGER', 'EMPLOYEE'] });
    let b64 = btoa(payloadJson);
    b64 = b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    const token = `h.${b64}.s`;
    sessionStorage.setItem('access_token', token);
    const auth = TestBed.inject(AuthService);
    expect(auth.roles()).toEqual(['MANAGER', 'EMPLOYEE']);
  });
});
