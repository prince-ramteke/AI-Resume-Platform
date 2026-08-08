import apiClient from "./client";
import type {
  LoginRequest,
  LoginResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
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

/** POST /api/auth/login — returns an access token only (no user object). */
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
