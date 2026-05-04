import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface JwtLoginResponse {
  token: string;
}

const STORAGE_KEY = 'access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  login(username: string, password: string): Observable<JwtLoginResponse> {
    return this.http
      .post<JwtLoginResponse>(`${environment.apiBase}/api/v1/auth/login`, {
        username,
        password,
      })
      .pipe(tap((r) => sessionStorage.setItem(STORAGE_KEY, r.token)));
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    void this.router.navigateByUrl('/login');
  }

  get token(): string | null {
    return sessionStorage.getItem(STORAGE_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.token;
  }

  roles(): string[] {
    const t = this.token;
    if (!t) {
      return [];
    }
    try {
      const payload = decodePayload(t);
      const roles = payload['roles'];
      return Array.isArray(roles) ? (roles as string[]) : [];
    } catch {
      return [];
    }
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  username(): string | null {
    const t = this.token;
    if (!t) {
      return null;
    }
    try {
      const payload = decodePayload(t);
      const sub = payload['sub'];
      return typeof sub === 'string' ? sub : null;
    } catch {
      return null;
    }
  }
}

function decodePayload(token: string): Record<string, unknown> {
  const part = token.split('.')[1];
  let base64 = part.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) {
    base64 += '=';
  }
  const json = atob(base64);
  return JSON.parse(json) as Record<string, unknown>;
}
