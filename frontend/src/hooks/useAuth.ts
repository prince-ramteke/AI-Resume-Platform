import { useContext } from "react";
import { AuthContext } from "../context/AuthContext";
import type { AuthState } from "../context/AuthContext";

/** Access auth state/actions. Throws if used outside <AuthProvider>. */
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (ctx === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
