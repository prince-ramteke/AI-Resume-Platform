import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "../../primitives/Button";
import { Input } from "../../primitives/Input";
import { Eyebrow } from "../../primitives/Eyebrow";
import { DocumentPaper } from "../../pipeline/DocumentPaper";
import { useLogin } from "../../../hooks/useLogin";
import { validateEmail, validateLoginPassword } from "../../../lib/validators";

export function LoginPage() {
  const { submit, isSubmitting, error } = useLogin();
  const [params] = useSearchParams();
  const justRegistered = params.get("registered") === "1";
  const justVerified = params.get("verified") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting) return;
    const emailErr = validateEmail(email);
    const passwordErr = validateLoginPassword(password);
    setFieldErrors({ email: emailErr, password: passwordErr });
    if (emailErr || passwordErr) return;
    void submit(email.trim(), password);
  }

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
        <Eyebrow>Resume Intelligence / Sign in</Eyebrow>
        <h1 style={{ marginTop: 20, fontSize: "var(--app-fs-h1)", lineHeight: "var(--app-lh-h1)" }}>
          Welcome back.
        </h1>
        <p style={{ marginTop: 12, color: "var(--fg-3)", fontSize: 15, lineHeight: 1.6 }}>
          Sign in to open your workspace and continue where you left off.
        </p>

        {justVerified && (
          <div style={{ marginTop: 24, padding: "12px 14px", border: "1px solid var(--signal-pass)", color: "var(--signal-pass)", borderRadius: "var(--r-control)", fontFamily: "var(--font-mono)", fontSize: 12.5, letterSpacing: ".02em" }}>
            Email verified. Sign in to continue.
          </div>
        )}
        {!justVerified && justRegistered && (
          <div style={{ marginTop: 24, padding: "12px 14px", border: "1px solid var(--signal-pass)", color: "var(--signal-pass)", borderRadius: "var(--r-control)", fontFamily: "var(--font-mono)", fontSize: 12.5, letterSpacing: ".02em" }}>
            Account created. Sign in with your new credentials.
          </div>
        )}
        {error && (
          <div role="alert" style={{ marginTop: 24, padding: "12px 14px", border: "1px solid var(--signal-fail)", color: "var(--signal-fail)", borderRadius: "var(--r-control)", fontFamily: "var(--font-mono)", fontSize: 12.5, letterSpacing: ".02em" }}>
            {error}
          </div>
        )}

        <form onSubmit={onSubmit} noValidate style={{ marginTop: 32, display: "flex", flexDirection: "column", gap: 20 }}>
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            disabled={isSubmitting}
            required
          />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            disabled={isSubmitting}
            required
          />
          <div style={{ display: "flex", flexDirection: "column", gap: 12, marginTop: 8 }}>
            <Button type="submit" variant="primary" full disabled={isSubmitting}>
              {isSubmitting ? "Signing in…" : "Sign in →"}
            </Button>
            <p style={{ color: "var(--fg-3)", fontSize: 13.5 }}>
              Don't have an account?{" "}
              <Link to="/register" style={{ color: "var(--cyan-500)" }}>
                Create one
              </Link>
              .
            </p>
          </div>
        </form>
      </div>

      <div style={{ display: "flex", justifyContent: "center" }} className="app-auth-stage">
        <DocumentPaper width={340} lines={18} highlights={[2, 6, 11, 15]} />
      </div>

      <style>{`
        @media (max-width: 900px){
          .app-auth-stage { display: none !important; }
        }
      `}</style>
    </div>
  );
}
