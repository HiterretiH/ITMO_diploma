import { AuditEventResponse } from '../core/audit.models';
import { OrderResponse } from '../core/order.models';

/** Mirrors backend OrderService.validateReadyForComplete */
export function orderReadyForBackendComplete(o: OrderResponse): boolean {
  return (
    o.vehicleId != null &&
    o.driverId != null &&
    (o.loadingPlace ?? '').trim().length > 0 &&
    (o.unloadingPlace ?? '').trim().length > 0 &&
    (o.orderDate ?? '').trim().length > 0 &&
    o.totalPrice != null
  );
}

export function orderMarkedCompleted(events: AuditEventResponse[]): boolean {
  return events.some((e) => e.eventType === 'ORDER_COMPLETED');
}

export type OrderUiPhase = 'draft' | 'ready' | 'completed';

export function orderUiPhase(
  order: OrderResponse,
  completed: boolean,
): OrderUiPhase {
  if (completed) {
    return 'completed';
  }
  if (orderReadyForBackendComplete(order)) {
    return 'ready';
  }
  return 'draft';
}
