import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserCreateRequest, UserResponse } from './user.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  createUser(body: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/admin/users`, body);
  }
}
