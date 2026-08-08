import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register as registerRequest } from "../api/auth";
import { parseApiError } from "../api/errors";

export interface RegisterError {
  message: string;
  /** True when the email is already taken (409) — the page offers a sign-in link. */
  emailTaken: boolean;
}

/**
 * Registration orchestration: POST /register → on 201 redirect to
 * /login?registered=1 (no token is returned, so no auto-login). A 409 becomes a
 * friendly "already registered" message — the raw backend message is not shown
 * because it echoes the submitted email back.
 */
export function useRegister() {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<RegisterError | null>(null);

  async function submit(email: string, password: string): Promise<void> {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await registerRequest({ email, password });
      navigate("/login?registered=1", { replace: true });
    } catch (err) {
      const { status, message } = parseApiError(err);
      if (status === 409) {
        setError({
          message: "That email is already registered.",
          emailTaken: true,
        });
      } else {
        setError({ message, emailTaken: false });
      }
      setIsSubmitting(false);
    }
  }

  return { submit, isSubmitting, error };
}
