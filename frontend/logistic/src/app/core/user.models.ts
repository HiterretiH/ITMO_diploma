/** Mirrors api/openapi.yaml Role, UserCreateRequest, UserResponse */

export type Role = 'USER' | 'ADMIN';

export interface UserCreateRequest {
  username: string;
  password: string;
  roles: Role[];
}

export interface UserResponse {
  id: number;
  username: string;
  roles: Role[];
}
