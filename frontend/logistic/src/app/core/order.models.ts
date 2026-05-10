/** Orders & documents — api/openapi.yaml */

export interface OrderCreateRequest {
  customerId: number;
  performerId: number;
  vehicleId?: number | null;
  driverId?: number | null;
  orderDate?: string;
  orderNumber?: number;
  loadingPlace?: string;
  loadingContact?: string;
  unloadingPlace?: string;
  unloadingContact?: string;
  tripCount?: number;
  pricePerTrip?: number;
  totalPrice?: number | null;
}

export interface OrderUpdateRequest {
  customerId?: number;
  performerId?: number;
  vehicleId?: number;
  driverId?: number;
  orderDate?: string;
  orderNumber?: number;
  loadingPlace?: string;
  loadingContact?: string;
  unloadingPlace?: string;
  unloadingContact?: string;
  tripCount?: number;
  pricePerTrip?: number;
  totalPrice?: number;
}

export interface OrderResponse {
  id: number;
  customerId: number;
  performerId: number;
  vehicleId: number | null;
  driverId: number | null;
  orderNumber: number;
  orderDate: string;
  loadingPlace: string;
  loadingContact: string | null;
  unloadingPlace: string;
  unloadingContact: string | null;
  tripCount: number;
  pricePerTrip: number | null;
  totalPrice: number | null;
  templateVersion: number;
  completed: boolean;
  customerShortName: string;
  performerShortName: string;
}

export type DocumentTypeName =
  | 'CONTRACT_APPLICATION'
  | 'WAYBILL'
  | 'ACT_OF_WORK';

export type FileFormatName = 'PDF' | 'DOCX';

/** GET /api/v1/me/trip-form-draft */
export interface TripFormDraftResponse {
  nextOrderNumber: number;
  lastPerformerId: number | null;
  lastDriverId: number | null;
  lastVehicleId: number | null;
}

export interface OrderDocumentDescriptor {
  documentType: DocumentTypeName;
  formats: FileFormatName[];
  requiresCompleteData: boolean;
}
