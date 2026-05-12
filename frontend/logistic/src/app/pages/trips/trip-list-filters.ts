import { OrderResponse } from '../../core/order.models';

export interface CompletedTripsDateFilter {
  dateFromInclusive: string | null;
  dateToInclusive: string | null;
}

/** `customerShortName` empty string or null means no filter. Dates are yyyy-mm-dd inclusive. */
export function filterCompletedTrips(
  completed: OrderResponse[],
  customerShortName: string | null | undefined,
  dates: CompletedTripsDateFilter,
): OrderResponse[] {
  const cust = (customerShortName ?? '').trim();
  return completed.filter((o) => {
    if (cust !== '') {
      const n = (o.customerShortName ?? '').trim();
      if (n !== cust) {
        return false;
      }
    }
    const d = (o.orderDate ?? '').slice(0, 10);
    if (d.length >= 10) {
      if (dates.dateFromInclusive && d < dates.dateFromInclusive) {
        return false;
      }
      if (dates.dateToInclusive && d > dates.dateToInclusive) {
        return false;
      }
    }
    return true;
  });
}

export interface CompletedTripsStats {
  count: number;
  /** Sum where totalPrice is non-null; null if no such rows. */
  totalPriceSum: number | null;
}

export function summarizeCompletedTrips(
  rows: OrderResponse[],
): CompletedTripsStats {
  if (rows.length === 0) {
    return { count: 0, totalPriceSum: null };
  }
  let sum = 0;
  let had = false;
  for (const o of rows) {
    if (o.totalPrice != null && Number.isFinite(Number(o.totalPrice))) {
      had = true;
      sum += Number(o.totalPrice);
    }
  }
  return { count: rows.length, totalPriceSum: had ? sum : null };
}
