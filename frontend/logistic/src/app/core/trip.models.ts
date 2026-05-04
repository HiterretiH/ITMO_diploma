export type TripStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'ARCHIVED';

export interface TripResponse {
  id: number;
  ownerId: number;
  ownerUsername: string;
  status: TripStatus;
  shipperId: number | null;
  consigneeId: number | null;
  driverId: number | null;
  vehicleId: number | null;
  cargoDescription: string | null;
  cargoWeightKg: number | string | null;
  routeFrom: string | null;
  routeTo: string | null;
  loadDate: string | null;
  unloadDate: string | null;
  priceAmount: number | string | null;
  currency: string | null;
  updatedAt: string;
}

export interface TripUpdateRequest {
  shipperId?: number | null;
  consigneeId?: number | null;
  driverId?: number | null;
  vehicleId?: number | null;
  cargoDescription?: string | null;
  cargoWeightKg?: number | null;
  routeFrom?: string | null;
  routeTo?: string | null;
  loadDate?: string | null;
  unloadDate?: string | null;
  priceAmount?: number | null;
  currency?: string | null;
}

export interface GeneratedDocumentResponse {
  id: number;
  documentType: string;
  fileFormat: string;
  sha256: string;
  createdAt: string;
}

