import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { ReactNode } from "react";
import { setAuthHandlers } from "../api/client";
import type { AuthUser } from "../types";

export interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (token: string, refreshToken: string, user: AuthUser) => void;
  logout: () => void;
  refresh: (newToken: string, newRefreshToken: string) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthState | undefined>(undefined);

/**
 * Holds the JWT and user in memory only — never localStorage/sessionStorage
 * (the bundle is public; see docs/SECURITY.md and rules/frontend). A page
 * refresh therefore logs the user out; ProtectedRoute preserves the intended
 * path via `?next=` so they return after signing in again.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);

  // Ref mirrors the token so the request interceptor always reads the latest
  // value without re-registering handlers on every token change.
  const tokenRef = useRef<string | null>(null);
  const refreshTokenRef = useRef<string | null>(null);
  tokenRef.current = token;
  refreshTokenRef.current = refreshToken;

  const login = useCallback((newToken: string, newRefreshToken: string, newUser: AuthUser) => {
    setToken(newToken);
    setRefreshToken(newRefreshToken);
    setUser(newUser);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setRefreshToken(null);
    setUser(null);
  }, []);

  const refresh = useCallback((newToken: string, newRefreshToken: string) => {
    setToken(newToken);
    setRefreshToken(newRefreshToken);
  }, []);

  useEffect(() => {
    setAuthHandlers({
      getToken: () => tokenRef.current,
      getRefreshToken: () => refreshTokenRef.current,
      onUnauthorized: logout,
    });
  }, [logout]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      refreshToken,
      user,
      isAuthenticated: token !== null,
      login,
      logout,
      refresh,
    }),
    [token, refreshToken, user, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
