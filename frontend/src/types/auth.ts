import type { AuthUser, Role } from "./index";

/** POST /api/auth/register request body. */
export interface RegisterRequest {
  email: string;
  password: string;
}

/** POST /api/auth/register 201 response. */
export interface RegisterResponse {
  id: number;
  email: string;
  role: Role;
}

/** POST /api/auth/login request body. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** POST /api/auth/login 200 response. */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

/** GET /api/auth/me 200 response. */
export type MeResponse = AuthUser;
