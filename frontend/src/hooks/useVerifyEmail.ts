import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { verifyEmail as verifyEmailApi, resendOtp as resendOtpApi } from "../api/auth";
import { parseApiError } from "../api/errors";
import type { AxiosError } from "axios";

const DEFAULT_COOLDOWN_SECONDS = 60;

export interface UseVerifyEmailReturn {
  otp: string;
  setOtp: (v: string) => void;
  isSubmitting: boolean;
  isResending: boolean;
  error: string | null;
  /** Seconds remaining before resend is allowed. 0 = can resend now. */
  cooldownSeconds: number;
  submit: () => Promise<void>;
  resend: () => Promise<void>;
}

/**
 * Orchestrates the email-verification flow: OTP submission, resend with
 * cooldown, and error classification. The email is treated as a display
 * hint only — never stored; the OTP is never logged.
 */
export function useVerifyEmail(email: string): UseVerifyEmailReturn {
  const navigate = useNavigate();
  const [otp, setOtpRaw] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  // Synchronous guard — updated before the first await so any concurrent caller
  // (auto-submit effect + button click in the same render cycle) sees it immediately,
  // unlike the isSubmitting React state which only propagates after the next commit.
  const submittingRef = useRef(false);

  function startCooldown(seconds: number) {
    if (timerRef.current) clearInterval(timerRef.current);
    setCooldownSeconds(seconds);
    timerRef.current = setInterval(() => {
      setCooldownSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current!);
          timerRef.current = null;
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }

  useEffect(() => () => { if (timerRef.current) clearInterval(timerRef.current); }, []);

  function setOtp(value: string) {
    // Accept only digits, max 6.
    setOtpRaw(value.replace(/\D/g, "").slice(0, 6));
    setError(null);
  }

  const submit = useCallback(async () => {
    if (submittingRef.current || otp.length !== 6) return;
    submittingRef.current = true;
    setIsSubmitting(true);
    setError(null);
    try {
      await verifyEmailApi({ email, otp });
      navigate("/login?verified=1", { replace: true });
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string; retryAfterSeconds?: number }>;
      const status = axiosErr.response?.status;
      const body = axiosErr.response?.data;

      if (status === 429 && body?.retryAfterSeconds) {
        startCooldown(body.retryAfterSeconds);
        setError("Too many attempts. Please wait before trying again.");
      } else {
        const { message } = parseApiError(err);
        setError(message);
      }
      setIsSubmitting(false);
    } finally {
      submittingRef.current = false;
    }
  }, [email, navigate, otp]);

  const resend = useCallback(async () => {
    if (isResending || cooldownSeconds > 0) return;
    setIsResending(true);
    setError(null);
    try {
      await resendOtpApi({ email });
      startCooldown(DEFAULT_COOLDOWN_SECONDS);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string; retryAfterSeconds?: number }>;
      const body = axiosErr.response?.data;
      if (body?.retryAfterSeconds) {
        startCooldown(body.retryAfterSeconds);
      }
      const { message } = parseApiError(err);
      setError(message);
    } finally {
      setIsResending(false);
    }
  }, [email, cooldownSeconds, isResending]);

  return { otp, setOtp, isSubmitting, isResending, error, cooldownSeconds, submit, resend };
}
