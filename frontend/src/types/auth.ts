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
  /** Present (and true) when EMAIL_VERIFICATION_ENABLED=true on the backend. */
  emailVerificationRequired?: boolean;
}

/** POST /api/auth/verify-email request body. */
export interface VerifyEmailRequest {
  email: string;
  otp: string;
}

/** POST /api/auth/verify-email 200 response. */
export interface VerifyEmailResponse {
  message: string;
}

/** POST /api/auth/resend-otp request body. */
export interface ResendOtpRequest {
  email: string;
}

/** POST /api/auth/resend-otp 200 response. */
export interface ResendOtpResponse {
  message: string;
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
  refreshToken: string;
  refreshExpiresAt: string;
}

/** GET /api/auth/me 200 response. */
export type MeResponse = AuthUser;
