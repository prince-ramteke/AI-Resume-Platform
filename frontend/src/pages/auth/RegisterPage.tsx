import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { AuthLayout } from "../../components/layout/AuthLayout";
import { Alert, Button, Input } from "../../components/ui";
import { useRegister } from "../../hooks/useRegister";
import {
  validateConfirm,
  validateEmail,
  validatePassword,
} from "../../lib/validators";

export function RegisterPage() {
  const { submit, isSubmitting, error } = useRegister();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    email?: string;
    password?: string;
    confirm?: string;
  }>({});

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting) return;

    const emailErr = validateEmail(email);
    const passwordErr = validatePassword(password);
    const confirmErr = validateConfirm(password, confirm);
    setFieldErrors({
      email: emailErr,
      password: passwordErr,
      confirm: confirmErr,
    });
    if (emailErr || passwordErr || confirmErr) return;

    void submit(email.trim(), password);
  }

  return (
    <AuthLayout
      title="Create account"
      subtitle="Start scoring resumes against job descriptions."
    >
      {error && (
        <div className="mb-5">
          <Alert tone="error">
            {error.message}
            {error.emailTaken && (
              <>
                {" "}
                <Link
                  to="/login"
                  className="font-medium underline hover:no-underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                >
                  Sign in instead
                </Link>
                .
              </>
            )}
          </Alert>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate className="space-y-5">
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
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          helperText="At least 8 characters, with a letter and a number."
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
        <Button type="submit" className="w-full" isLoading={isSubmitting}>
          Create account
        </Button>
      </form>

      <p className="mt-6 text-sm text-muted">
        Already have an account?{" "}
        <Link
          to="/login"
          className="font-medium text-accent hover:text-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        >
          Sign in
        </Link>
        .
      </p>
    </AuthLayout>
  );
}
