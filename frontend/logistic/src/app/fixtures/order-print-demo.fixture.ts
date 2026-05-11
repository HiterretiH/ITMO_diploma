/**
 * Trip form values aligned with backend manual-review OrderPrintSnapshot / DocumentFixtureGenerator.sampleSnapshot.
 */
export const ORDER_PRINT_DEMO_ORDER_DATE = new Date(2026, 4, 15, 12, 0, 0, 0);

/** Route, contacts, date, and money (single leg) — same semantics as OrderPrintSnapshot fields. */
export const orderPrintDemoTripFormPatch = {
  loadingPlace: 'Москва',
  loadingContact: 'Контакт погрузки +7 900 123 45 67',
  unloadingPlace: 'Санкт-Петербург',
  unloadingContact: 'Контакт разгрузки +7 900 765 43 21',
  orderDate: ORDER_PRINT_DEMO_ORDER_DATE,
  legCount: 1,
  ratePerLeg: 98500,
  orderNumber: 42,
} as const;

/** Typical catalog ids used in unit tests when lists are stubbed. */
export const orderPrintDemoCatalogIds = {
  customerId: 1,
  performerId: 2,
  driverId: 1,
  vehicleId: 2,
} as const;
