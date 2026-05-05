import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

/** Разрешает вход, если у пользователя есть хотя бы одна из ролей. */
export function roleGuard(...allowedRoles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (allowedRoles.some((r) => auth.hasRole(r))) {
      return true;
    }
    return router.parseUrl('/forbidden');
  };
}
