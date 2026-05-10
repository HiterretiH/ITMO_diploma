import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PasswordChangeRequest, UserResponse } from './user.models';

@Injectable({ providedIn: 'root' })
export class MeApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1/me`;

  getProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.base);
  }

  changePassword(body: PasswordChangeRequest): Observable<void> {
    return this.http.put<void>(`${this.base}/password`, body);
  }
}
