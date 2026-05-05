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

  updateCounterparty(
    id: number,
    body: {
      name: string;
      inn?: string | null;
      legalAddress?: string | null;
      phone?: string | null;
    },
  ): Observable<CounterpartyResponse> {
    return this.http.put<CounterpartyResponse>(
      `${this.base}/counterparties/${id}`,
      body,
    );
  }

  deleteCounterparty(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/counterparties/${id}`);
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

  updateDriver(
    id: number,
    body: {
      fullName: string;
      licenseNumber: string;
      licenseCategory?: string | null;
    },
  ): Observable<DriverResponse> {
    return this.http.put<DriverResponse>(`${this.base}/drivers/${id}`, body);
  }

  deleteDriver(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/drivers/${id}`);
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

  updateVehicle(
    id: number,
    body: {
      plateNumber: string;
      model?: string | null;
      loadCapacityKg?: number | null;
    },
  ): Observable<VehicleResponse> {
    return this.http.put<VehicleResponse>(`${this.base}/vehicles/${id}`, body);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/vehicles/${id}`);
  }
}
