import apiClient from "./client";
import type {
  LoginRequest,
  LoginResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
  ResendOtpRequest,
  ResendOtpResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
} from "../types/auth";

/** POST /api/auth/register — creates a user. Returns no token (no auto-login). */
export async function register(
  body: RegisterRequest
): Promise<RegisterResponse> {
  const { data } = await apiClient.post<RegisterResponse>(
    "/auth/register",
    body
  );
  return data;
}

/** POST /api/auth/login — returns access + refresh tokens. */
export async function login(body: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>("/auth/login", body);
  return data;
}

/**
 * GET /api/auth/me — hydrates the authenticated user.
 *
 * During the login round-trip the context token isn't set yet, so the request
 * interceptor has nothing to attach. Pass the freshly minted token explicitly
 * here so the call is self-contained and the user + token can be committed to
 * context in one atomic `login()`.
 */
export async function getMe(token?: string): Promise<MeResponse> {
  const config = token
    ? { headers: { Authorization: `Bearer ${token}` } }
    : undefined;
  const { data } = await apiClient.get<MeResponse>("/auth/me", config);
  return data;
}

/** POST /api/auth/refresh — rotates the refresh token and returns a new access token. */
export async function refresh(refreshToken: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>("/auth/refresh", {
    refreshToken,
  });
  return data;
}

/** POST /api/auth/logout — revokes the refresh token server-side. */
export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post("/auth/logout", { refreshToken });
}

/** POST /api/auth/verify-email — consumes a one-time OTP and marks the account verified. */
export async function verifyEmail(body: VerifyEmailRequest): Promise<VerifyEmailResponse> {
  const { data } = await apiClient.post<VerifyEmailResponse>("/auth/verify-email", body);
  return data;
}

/** POST /api/auth/resend-otp — sends a fresh OTP to an unverified address. */
export async function resendOtp(body: ResendOtpRequest): Promise<ResendOtpResponse> {
  const { data } = await apiClient.post<ResendOtpResponse>("/auth/resend-otp", body);
  return data;
}
