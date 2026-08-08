import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import type { Role } from "../types";

/**
 * Gate for role-restricted routes (e.g. ADMIN). Built now so an admin area can
 * drop in later; no admin screens ship in M6. Assumes it sits inside
 * ProtectedRoute, so the user is already authenticated here.
 */
export function RoleRoute({ role }: { role: Role }) {
  const { user } = useAuth();

  if (user?.role !== role) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
