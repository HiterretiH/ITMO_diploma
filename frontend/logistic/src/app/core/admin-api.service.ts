import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RegistrationRequestResponse } from './auth.models';
import { UserCreateRequest, UserResponse } from './user.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  createUser(body: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/admin/users`, body);
  }

  listRegistrationRequests(): Observable<RegistrationRequestResponse[]> {
    return this.http.get<RegistrationRequestResponse[]>(
      `${this.base}/admin/registration-requests`,
    );
  }

  approveRegistrationRequest(id: number): Observable<RegistrationRequestResponse> {
    return this.http.post<RegistrationRequestResponse>(
      `${this.base}/admin/registration-requests/${id}/approve`,
      {},
    );
  }

  rejectRegistrationRequest(id: number): Observable<RegistrationRequestResponse> {
    return this.http.post<RegistrationRequestResponse>(
      `${this.base}/admin/registration-requests/${id}/reject`,
      {},
    );
  }
}
