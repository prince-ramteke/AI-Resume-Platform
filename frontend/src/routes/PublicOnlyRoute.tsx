import { Navigate, Outlet, useSearchParams } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

/** Only allow a same-origin absolute path (guards against open-redirect). */
function safeNext(next: string | null): string {
  if (next && next.startsWith("/") && !next.startsWith("//")) {
    return next;
  }
  return "/dashboard";
}

/**
 * Gate for auth-only pages (login/register). Already-authenticated users are
 * redirected away — to their `?next=` target when it's a safe local path,
 * otherwise to the dashboard.
 */
export function PublicOnlyRoute() {
  const { isAuthenticated } = useAuth();
  const [params] = useSearchParams();

  if (isAuthenticated) {
    return <Navigate to={safeNext(params.get("next"))} replace />;
  }

  return <Outlet />;
}
