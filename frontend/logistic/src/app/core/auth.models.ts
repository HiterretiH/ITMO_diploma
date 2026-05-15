/** Mirrors api/openapi.yaml components/schemas */

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  token: string;
}

export type RegistrationStatus = 'PENDING' | 'REJECTED' | 'APPROVED';

export interface RegisterResponse {
  username: string;
  registrationStatus: RegistrationStatus;
}

export interface RegistrationStatusResponse {
  username: string;
  registrationStatus: RegistrationStatus;
}

export interface RegistrationRequestResponse {
  id: number;
  username: string;
  registrationStatus: RegistrationStatus;
}
