import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  DocumentTypeName,
  FileFormatName,
  OrderCreateRequest,
  OrderDocumentDescriptor,
  OrderResponse,
  OrderUpdateRequest,
  TripFormDraftResponse,
} from './order.models';

export interface DocumentBlobDownload {
  blob: Blob;
  fileName: string;
}

function fileNameFromContentDisposition(header: string | null): string | null {
  if (!header) {
    return null;
  }
  const star = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(header);
  if (star?.[1]) {
    try {
      return decodeURIComponent(star[1].trim().replace(/^"+|"+$/g, ''));
    } catch {
      return star[1].trim().replace(/^"+|"+$/g, '');
    }
  }
  const quoted = /filename="([^"]+)"/i.exec(header);
  if (quoted?.[1]) {
    return quoted[1];
  }
  const plain = /filename=([^;]+)/i.exec(header);
  if (plain?.[1]) {
    return plain[1].trim().replace(/^"+|"+$/g, '');
  }
  return null;
}

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  /** Next application number and last picks for trip create form (current user). */
  getTripFormDraft(customerId?: number | null): Observable<TripFormDraftResponse> {
    let url = `${this.base}/me/trip-form-draft`;
    if (customerId != null) {
      url += `?customerId=${customerId}`;
    }
    return this.http.get<TripFormDraftResponse>(url);
  }

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
  ): Observable<DocumentBlobDownload> {
    const q = `format=${encodeURIComponent(format)}`;
    const url = `${this.base}/orders/${orderId}/documents/${documentType}/file?${q}`;
    const fallbackName = `${documentType.toLowerCase()}.${
      format === 'PDF' ? 'pdf' : 'docx'
    }`;
    return this.http
      .get(url, { responseType: 'blob', observe: 'response' })
      .pipe(
        map((res) => ({
          blob: res.body as Blob,
          fileName:
            fileNameFromContentDisposition(
              res.headers.get('content-disposition'),
            ) ?? fallbackName,
        })),
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
