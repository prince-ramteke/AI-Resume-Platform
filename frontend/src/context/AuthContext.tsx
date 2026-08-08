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
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
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
  const [user, setUser] = useState<AuthUser | null>(null);

  // Ref mirrors the token so the request interceptor always reads the latest
  // value without re-registering handlers on every token change.
  const tokenRef = useRef<string | null>(null);
  tokenRef.current = token;

  const login = useCallback((newToken: string, newUser: AuthUser) => {
    setToken(newToken);
    setUser(newUser);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    setAuthHandlers({
      getToken: () => tokenRef.current,
      onUnauthorized: logout,
    });
  }, [logout]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      user,
      isAuthenticated: token !== null,
      login,
      logout,
    }),
    [token, user, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
