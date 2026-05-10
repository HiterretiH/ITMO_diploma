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
