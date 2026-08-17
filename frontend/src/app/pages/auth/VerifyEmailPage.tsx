import { useEffect } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { Button } from "../../primitives/Button";
import { Eyebrow } from "../../primitives/Eyebrow";
import { useVerifyEmail } from "../../../hooks/useVerifyEmail";

function maskEmail(email: string): string {
  const atIndex = email.indexOf("@");
  if (atIndex < 0) return email;
  return `${email.slice(0, 1)}***@${email.slice(atIndex + 1)}`;
}

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const email = params.get("email") ?? "";

  // If no email in URL, send back to registration.
  if (!email) return <Navigate to="/register" replace />;

  return <VerifyEmailForm email={email} />;
}

function VerifyEmailForm({ email }: { email: string }) {
  const { otp, setOtp, isSubmitting, isResending, error, cooldownSeconds, submit, resend } =
    useVerifyEmail(email);

  // Auto-submit when all 6 digits are present.
  useEffect(() => {
    if (otp.length === 6) void submit();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [otp]);

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "minmax(0, 3fr) minmax(0, 2fr)",
        gap: 64,
        maxWidth: 1180,
        margin: "0 auto",
        padding: "48px 32px 96px",
        alignItems: "center",
        minHeight: "calc(100vh - var(--nav-offset))",
      }}
    >
      <div style={{ maxWidth: 460 }}>
        <Eyebrow>Resume Intelligence / Verify email</Eyebrow>
        <h1
          style={{ marginTop: 20, fontSize: "var(--app-fs-h1)", lineHeight: "var(--app-lh-h1)" }}
        >
          Check your inbox.
        </h1>
        <p style={{ marginTop: 12, color: "var(--fg-3)", fontSize: 15, lineHeight: 1.6 }}>
          We sent a 6-digit code to{" "}
          <span
            style={{ fontFamily: "var(--font-mono)", color: "var(--fg-2)" }}
            aria-label={`email address ${email}`}
          >
            {maskEmail(email)}
          </span>
          . Enter it below to verify your account.
        </p>

        {error && (
          <div
            role="alert"
            style={{
              marginTop: 24,
              padding: "12px 14px",
              border: "1px solid var(--signal-fail)",
              color: "var(--signal-fail)",
              borderRadius: "var(--r-control)",
              fontFamily: "var(--font-mono)",
              fontSize: 12.5,
              letterSpacing: ".02em",
            }}
          >
            {error}
          </div>
        )}

        <div style={{ marginTop: 32, display: "flex", flexDirection: "column", gap: 20 }}>
          {/* OTP input */}
          <div className="app-field-wrap">
            <label className="app-field-label" htmlFor="otp-input">
              Verification code
            </label>
            <input
              id="otp-input"
              className="app-field"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              disabled={isSubmitting}
              placeholder="000000"
              aria-invalid={error ? "true" : undefined}
              aria-describedby={error ? "otp-error" : undefined}
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 28,
                letterSpacing: "0.35em",
                textAlign: "center",
                paddingTop: 14,
                paddingBottom: 14,
              }}
            />
            {error && (
              <span id="otp-error" className="app-field-error">
                {error}
              </span>
            )}
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 12, marginTop: 8 }}>
            <Button
              type="button"
              variant="primary"
              full
              disabled={isSubmitting || otp.length !== 6}
              onClick={() => void submit()}
            >
              {isSubmitting ? "Verifying…" : "Verify email →"}
            </Button>

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                color: "var(--fg-3)",
                fontSize: 13.5,
              }}
            >
              <span>Didn't receive it?</span>
              {cooldownSeconds > 0 ? (
                <span
                  style={{ fontFamily: "var(--font-mono)", fontSize: 12.5, color: "var(--fg-4)" }}
                  aria-live="polite"
                >
                  Resend in {cooldownSeconds}s
                </span>
              ) : (
                <button
                  type="button"
                  onClick={() => void resend()}
                  disabled={isResending}
                  style={{
                    background: "none",
                    border: "none",
                    padding: 0,
                    cursor: isResending ? "default" : "pointer",
                    color: "var(--cyan-500)",
                    fontSize: 13.5,
                    fontFamily: "inherit",
                    textDecoration: "underline",
                    opacity: isResending ? 0.5 : 1,
                  }}
                  aria-label="Resend verification code"
                >
                  {isResending ? "Sending…" : "Resend code"}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Right-side decoration — hidden on narrow viewports */}
      <ol
        className="app-auth-stage"
        style={{
          listStyle: "none",
          padding: 0,
          margin: 0,
          display: "flex",
          flexDirection: "column",
          gap: 24,
          borderLeft: "1px solid var(--line-2)",
          paddingLeft: 24,
        }}
      >
        {[
          ["01", "Check your inbox"],
          ["02", "Enter the 6-digit code"],
          ["03", "Sign in and start analysing"],
        ].map(([num, label]) => (
          <li key={num} style={{ display: "flex", gap: 20, alignItems: "baseline" }}>
            <span
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 12.5,
                letterSpacing: ".14em",
                color: "var(--fg-4)",
              }}
            >
              {num}
            </span>
            <span
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 12.5,
                letterSpacing: ".14em",
                color: "var(--fg-2)",
                textTransform: "uppercase",
              }}
            >
              {label}
            </span>
          </li>
        ))}
      </ol>

      <style>{`
        @media (max-width: 900px) {
          .app-auth-stage { display: none !important; }
        }
      `}</style>
    </div>
  );
}
