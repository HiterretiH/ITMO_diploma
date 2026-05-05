export type AppRole = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';

export interface UserCreateRequest {
  username: string;
  password: string;
  roles: AppRole[];
}

export interface UserResponse {
  id: number;
  username: string;
  roles: AppRole[];
}
