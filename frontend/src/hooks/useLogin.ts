import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getMe, login as loginRequest } from "../api/auth";
import { parseApiError } from "../api/errors";
import { useAuth } from "./useAuth";

/**
 * Only allow a same-origin absolute path (guards against open-redirect).
 * Mirrors the guard in PublicOnlyRoute; kept local so this hook doesn't require
 * touching the M6.1 foundation. (Worth extracting to a shared lib in a later
 * polish pass — noted, not done here.)
 */
function safeNext(next: string | null): string {
  if (next && next.startsWith("/") && !next.startsWith("//")) {
    return next;
  }
  return "/dashboard";
}

/**
 * Login orchestration: POST /login → GET /me (token passed explicitly) →
 * commit both to context in one atomic `login()` → redirect to a safe `?next=`
 * target (default /dashboard). On success the page unmounts, so `isSubmitting`
 * is intentionally left true (the button stays disabled through navigation).
 */
export function useLogin() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(email: string, password: string): Promise<void> {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const { accessToken, refreshToken } = await loginRequest({ email, password });
      const user = await getMe(accessToken);
      login(accessToken, refreshToken, user);
      navigate(safeNext(params.get("next")), { replace: true });
    } catch (err) {
      const { status, message } = parseApiError(err);
      setError(status === 401 ? "Invalid email or password." : message);
      setIsSubmitting(false);
    }
  }

  return { submit, isSubmitting, error };
}
