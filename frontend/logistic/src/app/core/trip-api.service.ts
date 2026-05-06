import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  GeneratedDocumentResponse,
  TripResponse,
  TripUpdateRequest,
  TripStatus,
} from './trip.models';

@Injectable({ providedIn: 'root' })
export class TripApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/api/v1`;

  list(status?: TripStatus): Observable<TripResponse[]> {
    const q = status ? `?status=${encodeURIComponent(status)}` : '';
    return this.http.get<TripResponse[]>(`${this.base}/trips${q}`);
  }

  get(id: number): Observable<TripResponse> {
    return this.http.get<TripResponse>(`${this.base}/trips/${id}`);
  }

  create(): Observable<TripResponse> {
    return this.http.post<TripResponse>(`${this.base}/trips`, {});
  }

  update(id: number, body: TripUpdateRequest): Observable<TripResponse> {
    return this.http.put<TripResponse>(`${this.base}/trips/${id}`, body);
  }

  complete(id: number): Observable<TripResponse> {
    return this.http.post<TripResponse>(`${this.base}/trips/${id}/complete`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/trips/${id}`);
  }

  documents(id: number): Observable<GeneratedDocumentResponse[]> {
    return this.http.get<GeneratedDocumentResponse[]>(
      `${this.base}/trips/${id}/documents`,
    );
  }

  downloadDocumentUrl(docId: number): string {
    return `${this.base}/generated-documents/${docId}/file`;
  }

  downloadFile(docId: number) {
    return this.http.get(`${this.base}/generated-documents/${docId}/file`, {
      responseType: 'blob',
    });
  }
}
