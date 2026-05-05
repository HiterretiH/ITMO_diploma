export type AuditEventType =
  | 'TRIP_CREATED'
  | 'TRIP_UPDATED'
  | 'TRIP_SUBMITTED'
  | 'TRIP_APPROVED'
  | 'TRIP_ARCHIVED'
  | 'DOCUMENTS_GENERATED'
  | 'LOGIN'
  | 'REGISTER';

export interface AuditEventResponse {
  id: number;
  eventType: AuditEventType;
  payload: string | null;
  createdAt: string;
  userId: number | null;
}
