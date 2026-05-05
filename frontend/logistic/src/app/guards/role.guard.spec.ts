import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        AuthService,
        {
          provide: Router,
          useValue: {
            parseUrl: (u: string) => ({ toString: () => u }),
          },
        },
      ],
    });
    sessionStorage.clear();
  });

  it('allows when user has one of roles', () => {
    const payloadJson = JSON.stringify({ roles: ['MANAGER'] });
    let b64 = btoa(payloadJson);
    b64 = b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    sessionStorage.setItem('access_token', `h.${b64}.s`);
    const guard = TestBed.runInInjectionContext(() =>
      roleGuard('MANAGER', 'ADMIN')(
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
      ),
    );
    expect(guard).toBe(true);
  });

  it('denies when user lacks role', () => {
    const payloadJson = JSON.stringify({ roles: ['EMPLOYEE'] });
    let b64 = btoa(payloadJson);
    b64 = b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    sessionStorage.setItem('access_token', `h.${b64}.s`);
    const guard = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(guard === true).toBe(false);
  });
});
