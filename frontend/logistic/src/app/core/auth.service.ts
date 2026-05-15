import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  JwtResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  RegistrationStatusResponse,
} from './auth.models';

const STORAGE_KEY = 'access_token';
const PENDING_REGISTRATION_KEY = 'pending_registration_username';

/** @deprecated use JwtResponse from auth.models */
export type JwtLoginResponse = JwtResponse;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  login(username: string, password: string): Observable<JwtResponse> {
    const body: LoginRequest = { username, password };
    return this.http
      .post<JwtResponse>(`${environment.apiBase}/api/v1/auth/login`, body)
      .pipe(tap((r) => sessionStorage.setItem(STORAGE_KEY, r.token)));
  }

  register(username: string, password: string): Observable<RegisterResponse> {
    const body: RegisterRequest = { username, password };
    return this.http.post<RegisterResponse>(
      `${environment.apiBase}/api/v1/auth/register`,
      body,
    );
  }

  getRegistrationStatus(username: string): Observable<RegistrationStatusResponse> {
    const params = new HttpParams().set('username', username);
    return this.http.get<RegistrationStatusResponse>(
      `${environment.apiBase}/api/v1/auth/registration-status`,
      { params },
    );
  }

  setPendingRegistrationUsername(username: string): void {
    sessionStorage.setItem(PENDING_REGISTRATION_KEY, username);
  }

  getPendingRegistrationUsername(): string | null {
    return sessionStorage.getItem(PENDING_REGISTRATION_KEY);
  }

  clearPendingRegistrationUsername(): void {
    sessionStorage.removeItem(PENDING_REGISTRATION_KEY);
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
