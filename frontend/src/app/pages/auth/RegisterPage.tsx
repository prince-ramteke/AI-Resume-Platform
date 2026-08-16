import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../primitives/Button";
import { Input } from "../../primitives/Input";
import { Eyebrow } from "../../primitives/Eyebrow";
import { useRegister } from "../../../hooks/useRegister";
import { validateConfirm, validateEmail, validatePassword } from "../../../lib/validators";

export function RegisterPage() {
  const { submit, isSubmitting, error } = useRegister();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    email?: string; password?: string; confirm?: string;
  }>({});

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting) return;
    const emailErr = validateEmail(email);
    const passwordErr = validatePassword(password);
    const confirmErr = validateConfirm(password, confirm);
    setFieldErrors({ email: emailErr, password: passwordErr, confirm: confirmErr });
    if (emailErr || passwordErr || confirmErr) return;
    void submit(email.trim(), password);
  }

  const strength =
    password.length === 0 ? "" :
    password.length < 8 ? "weak" :
    /[^a-zA-Z0-9]/.test(password) ? "strong" : "ok";

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
        <Eyebrow>Resume Intelligence / Create account</Eyebrow>
        <h1 style={{ marginTop: 20, fontSize: "var(--app-fs-h1)", lineHeight: "var(--app-lh-h1)" }}>
          Start reading your resume the way machines do.
        </h1>
        <p style={{ marginTop: 12, color: "var(--fg-3)", fontSize: 15, lineHeight: 1.6 }}>
          Create an account to upload a resume, paste a job description, and see an
          evidence-backed analysis.
        </p>

        {error && (
          <div role="alert" style={{ marginTop: 24, padding: "12px 14px", border: "1px solid var(--signal-fail)", color: "var(--signal-fail)", borderRadius: "var(--r-control)", fontFamily: "var(--font-mono)", fontSize: 12.5 }}>
            {error.message}
            {error.emailTaken && (
              <>
                {" "}
                <Link to="/login" style={{ color: "var(--cyan-500)" }}>Sign in instead</Link>.
              </>
            )}
          </div>
        )}

        <form onSubmit={onSubmit} noValidate style={{ marginTop: 32, display: "flex", flexDirection: "column", gap: 20 }}>
          <Input
            label="Work email"
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
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            help={strength ? `strength · ${strength}` : "at least 8 characters, one letter and one number"}
            disabled={isSubmitting}
            required
          />
          <Input
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            error={fieldErrors.confirm}
            disabled={isSubmitting}
            required
          />
          <div style={{ display: "flex", flexDirection: "column", gap: 12, marginTop: 8 }}>
            <Button type="submit" variant="primary" full disabled={isSubmitting}>
              {isSubmitting ? "Creating account…" : "Create account →"}
            </Button>
            <p style={{ color: "var(--fg-3)", fontSize: 13.5 }}>
              Already have an account?{" "}
              <Link to="/login" style={{ color: "var(--cyan-500)" }}>Sign in</Link>.
            </p>
          </div>
        </form>
      </div>

      <ol
        className="app-auth-stage"
        style={{
          listStyle: "none", padding: 0, margin: 0,
          display: "flex", flexDirection: "column", gap: 24,
          borderLeft: "1px solid var(--line-2)", paddingLeft: 24,
        }}
      >
        {[
          ["01", "Upload resume"],
          ["02", "Paste job description"],
          ["03", "Receive evidence-backed analysis"],
        ].map(([num, label]) => (
          <li key={num} style={{ display: "flex", gap: 20, alignItems: "baseline" }}>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: 12.5, letterSpacing: ".14em", color: "var(--fg-4)" }}>{num}</span>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: 12.5, letterSpacing: ".14em", color: "var(--fg-2)", textTransform: "uppercase" }}>{label}</span>
          </li>
        ))}
      </ol>

      <style>{`
        @media (max-width: 900px){
          .app-auth-stage { display: none !important; }
        }
      `}</style>
    </div>
  );
}
