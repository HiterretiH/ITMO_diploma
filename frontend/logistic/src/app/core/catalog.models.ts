/** Catalog DTOs — api/openapi.yaml */

export interface CustomerRequest {
  shortName: string;
  fullName?: string | null;
  phone?: string | null;
  requisites?: string | null;
}

export interface CustomerResponse {
  id: number;
  shortName: string;
  fullName: string | null;
  phone: string | null;
  requisites: string | null;
}

export interface PerformerRequest {
  shortName: string;
  fullName?: string | null;
  phone?: string | null;
  bankName?: string | null;
  inn?: string | null;
  bik?: string | null;
  kpp?: string | null;
  paymentAccount?: string | null;
  corrAccount?: string | null;
  requisites?: string | null;
}

export interface PerformerResponse {
  id: number;
  shortName: string;
  fullName: string | null;
  phone: string | null;
  bankName: string | null;
  inn: string | null;
  bik: string | null;
  kpp: string | null;
  paymentAccount: string | null;
  corrAccount: string | null;
  requisites: string | null;
}

export interface DriverRequest {
  performerId: number;
  fullName: string;
  phone?: string | null;
  isDefault?: boolean | null;
}

export interface DriverResponse {
  id: number;
  performerId: number;
  fullName: string;
  phone: string | null;
  isDefault: boolean;
}

export interface VehicleRequest {
  performerId: number;
  brandModel?: string | null;
  plateNumber?: string | null;
  type?: string | null;
  isDefault?: boolean | null;
}

export interface VehicleResponse {
  id: number;
  performerId: number;
  brandModel: string | null;
  plateNumber: string | null;
  type: string | null;
  isDefault: boolean;
}
