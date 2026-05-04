import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  counterparties(q?: string): Observable<CounterpartyResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<CounterpartyResponse[]>(`${this.base}/counterparties${qs}`);
  }

  createCounterparty(body: {
    name: string;
    inn?: string | null;
    legalAddress?: string | null;
    phone?: string | null;
  }): Observable<CounterpartyResponse> {
    return this.http.post<CounterpartyResponse>(`${this.base}/counterparties`, body);
  }

  drivers(q?: string): Observable<DriverResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<DriverResponse[]>(`${this.base}/drivers${qs}`);
  }

  createDriver(body: {
    fullName: string;
    licenseNumber: string;
    licenseCategory?: string | null;
  }): Observable<DriverResponse> {
    return this.http.post<DriverResponse>(`${this.base}/drivers`, body);
  }

  vehicles(q?: string): Observable<VehicleResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<VehicleResponse[]>(`${this.base}/vehicles${qs}`);
  }

  createVehicle(body: {
    plateNumber: string;
    model?: string | null;
    loadCapacityKg?: number | null;
  }): Observable<VehicleResponse> {
    return this.http.post<VehicleResponse>(`${this.base}/vehicles`, body);
  }
}
