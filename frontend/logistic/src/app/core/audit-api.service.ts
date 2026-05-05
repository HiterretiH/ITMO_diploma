import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditEventResponse } from './audit.models';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  listByTrip(tripId: number): Observable<AuditEventResponse[]> {
    return this.http.get<AuditEventResponse[]>(
      `${this.base}/trips/${tripId}/audit-events`,
    );
  }
}
