import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CustomerRequest,
  CustomerResponse,
  CustomerRouteHintResponse,
  DriverRequest,
  DriverResponse,
  PerformerRequest,
  PerformerResponse,
  RouteHintKind,
  VehicleRequest,
  VehicleResponse,
} from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  listCustomers(q?: string): Observable<CustomerResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<CustomerResponse[]>(`${this.base}/customers${qs}`);
  }

  getCustomer(id: number): Observable<CustomerResponse> {
    return this.http.get<CustomerResponse>(`${this.base}/customers/${id}`);
  }

  createCustomer(body: CustomerRequest): Observable<CustomerResponse> {
    return this.http.post<CustomerResponse>(`${this.base}/customers`, body);
  }

  updateCustomer(id: number, body: CustomerRequest): Observable<CustomerResponse> {
    return this.http.put<CustomerResponse>(`${this.base}/customers/${id}`, body);
  }

  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/customers/${id}`);
  }

  getCustomerRouteHints(
    customerId: number,
    kind: RouteHintKind,
    q?: string,
  ): Observable<CustomerRouteHintResponse[]> {
    const parts = [`kind=${encodeURIComponent(kind)}`];
    if (q != null && q.trim() !== '') {
      parts.push(`q=${encodeURIComponent(q.trim())}`);
    }
    return this.http.get<CustomerRouteHintResponse[]>(
      `${this.base}/customers/${customerId}/route-hints?${parts.join('&')}`,
    );
  }

  listPerformers(q?: string): Observable<PerformerResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<PerformerResponse[]>(`${this.base}/performers${qs}`);
  }

  getPerformer(id: number): Observable<PerformerResponse> {
    return this.http.get<PerformerResponse>(`${this.base}/performers/${id}`);
  }

  createPerformer(body: PerformerRequest): Observable<PerformerResponse> {
    return this.http.post<PerformerResponse>(`${this.base}/performers`, body);
  }

  updatePerformer(id: number, body: PerformerRequest): Observable<PerformerResponse> {
    return this.http.put<PerformerResponse>(`${this.base}/performers/${id}`, body);
  }

  deletePerformer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/performers/${id}`);
  }

  listDrivers(q?: string): Observable<DriverResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<DriverResponse[]>(`${this.base}/drivers${qs}`);
  }

  getDriver(id: number): Observable<DriverResponse> {
    return this.http.get<DriverResponse>(`${this.base}/drivers/${id}`);
  }

  createDriver(body: DriverRequest): Observable<DriverResponse> {
    return this.http.post<DriverResponse>(`${this.base}/drivers`, body);
  }

  updateDriver(id: number, body: DriverRequest): Observable<DriverResponse> {
    return this.http.put<DriverResponse>(`${this.base}/drivers/${id}`, body);
  }

  deleteDriver(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/drivers/${id}`);
  }

  listVehicles(q?: string): Observable<VehicleResponse[]> {
    const qs = q ? `?q=${encodeURIComponent(q)}` : '';
    return this.http.get<VehicleResponse[]>(`${this.base}/vehicles${qs}`);
  }

  getVehicle(id: number): Observable<VehicleResponse> {
    return this.http.get<VehicleResponse>(`${this.base}/vehicles/${id}`);
  }

  createVehicle(body: VehicleRequest): Observable<VehicleResponse> {
    return this.http.post<VehicleResponse>(`${this.base}/vehicles`, body);
  }

  updateVehicle(id: number, body: VehicleRequest): Observable<VehicleResponse> {
    return this.http.put<VehicleResponse>(`${this.base}/vehicles/${id}`, body);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/vehicles/${id}`);
  }
}
