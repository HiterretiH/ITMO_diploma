import { OrderResponse } from '../../core/order.models';
import {
  CompletedTripsStats,
  filterCompletedTrips,
  summarizeCompletedTrips,
} from './trip-list-filters';

function o(partial: Partial<OrderResponse>): OrderResponse {
  return {
    id: partial.id ?? 1,
    customerId: partial.customerId ?? 1,
    performerId: partial.performerId ?? 1,
    vehicleId: partial.vehicleId ?? null,
    driverId: partial.driverId ?? null,
    orderNumber: partial.orderNumber ?? 1,
    orderDate: partial.orderDate ?? '2025-01-15',
    loadingPlace: partial.loadingPlace ?? 'A',
    loadingContact: partial.loadingContact ?? null,
    unloadingPlace: partial.unloadingPlace ?? 'B',
    unloadingContact: partial.unloadingContact ?? null,
    tripCount: partial.tripCount ?? 1,
    pricePerTrip: partial.pricePerTrip ?? null,
    totalPrice: partial.totalPrice ?? null,
    templateVersion: partial.templateVersion ?? 1,
    completed: partial.completed ?? true,
    customerShortName: partial.customerShortName ?? 'C1',
    performerShortName: partial.performerShortName ?? 'P1',
  };
}

describe('trip-list-filters', () => {
  const rows: OrderResponse[] = [
    o({
      id: 1,
      customerShortName: 'Alpha',
      orderDate: '2025-01-10',
      totalPrice: 100,
    }),
    o({
      id: 2,
      customerShortName: 'Beta',
      orderDate: '2025-02-01',
      totalPrice: 200,
    }),
    o({
      id: 3,
      customerShortName: 'Alpha',
      orderDate: '2025-01-20',
      totalPrice: null,
    }),
  ];

  it('filterCompletedTrips by customer', () => {
    const r = filterCompletedTrips(rows, 'Alpha', {
      dateFromInclusive: null,
      dateToInclusive: null,
    });
    expect(r.map((x) => x.id)).toEqual([1, 3]);
  });

  it('filterCompletedTrips by date range inclusive', () => {
    const r = filterCompletedTrips(rows, '', {
      dateFromInclusive: '2025-01-15',
      dateToInclusive: '2025-01-25',
    });
    expect(r.map((x) => x.id)).toEqual([3]);
  });

  it('summarizeCompletedTrips sums finite totalPrice only', () => {
    const s: CompletedTripsStats = summarizeCompletedTrips(rows);
    expect(s.count).toBe(3);
    expect(s.totalPriceSum).toBe(300);
    const onlyNull = summarizeCompletedTrips([o({ id: 9, totalPrice: null })]);
    expect(onlyNull.totalPriceSum).toBeNull();
  });

  it('summarizeCompletedTrips skips non-finite totalPrice', () => {
    const row = { ...o({ id: 10, totalPrice: 0 }), totalPrice: Number.NaN } as OrderResponse;
    const s = summarizeCompletedTrips([row]);
    expect(s.count).toBe(1);
    expect(s.totalPriceSum).toBeNull();
  });

  it('filterCompletedTrips keeps rows when orderDate is too short', () => {
    const r = filterCompletedTrips([o({ id: 1, orderDate: '' })], '', {
      dateFromInclusive: '2025-01-01',
      dateToInclusive: '2025-12-31',
    });
    expect(r).toHaveLength(1);
  });
});
