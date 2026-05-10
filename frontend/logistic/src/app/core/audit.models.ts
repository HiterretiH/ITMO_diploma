/** api/openapi.yaml AuditEventResponse.eventType */

export type AuditEventType =
  | 'REGISTER'
  | 'ORDER_CREATED'
  | 'ORDER_UPDATED'
  | 'ORDER_COMPLETED'
  | 'ORDER_DELETED'
  | 'DOCUMENTS_GENERATED'
  | 'LOGIN';

export interface AuditEventResponse {
  id: number;
  eventType: AuditEventType;
  payload: string | null;
  createdAt: string;
  userId: number | null;
}
