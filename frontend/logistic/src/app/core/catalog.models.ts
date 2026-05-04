export interface CounterpartyResponse {
  id: number;
  name: string;
  inn: string | null;
  legalAddress: string | null;
  phone: string | null;
}

export interface DriverResponse {
  id: number;
  fullName: string;
  licenseNumber: string;
  licenseCategory: string | null;
}

export interface VehicleResponse {
  id: number;
  plateNumber: string;
  model: string | null;
  loadCapacityKg: number | null;
}
