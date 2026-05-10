import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  DocumentTypeName,
  FileFormatName,
  OrderCreateRequest,
  OrderDocumentDescriptor,
  OrderResponse,
  OrderUpdateRequest,
} from './order.models';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  list(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.base}/orders`);
  }

  get(id: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`${this.base}/orders/${id}`);
  }

  create(body: OrderCreateRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders`, body);
  }

  update(id: number, body: OrderUpdateRequest): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`${this.base}/orders/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/orders/${id}`);
  }

  complete(id: number): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/${id}/complete`, {});
  }

  listDocuments(orderId: number): Observable<OrderDocumentDescriptor[]> {
    return this.http.get<OrderDocumentDescriptor[]>(
      `${this.base}/orders/${orderId}/documents`,
    );
  }

  downloadDocument(
    orderId: number,
    documentType: DocumentTypeName,
    format: FileFormatName,
  ): Observable<Blob> {
    const q = `format=${encodeURIComponent(format)}`;
    return this.http.get(
      `${this.base}/orders/${orderId}/documents/${documentType}/file?${q}`,
      { responseType: 'blob' },
    );
  }

  /** Absolute URL for tools that need a string (caller must attach Authorization separately). */
  downloadDocumentUrl(
    orderId: number,
    documentType: DocumentTypeName,
    format: FileFormatName,
  ): string {
    const q = `format=${encodeURIComponent(format)}`;
    return `${this.base}/orders/${orderId}/documents/${documentType}/file?${q}`;
  }
}
