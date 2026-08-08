import { greetingForHour, displayNameFromEmail } from "../../lib/formatDate";

interface DashboardHeaderProps {
  email: string | undefined;
}

/**
 * Contextual greeting. The name is derived client-side from the email's
 * local-part (no displayName exists in the contract); when it can't produce a
 * reasonable label, it falls back to a plain "Welcome back". Pure/presentational.
 */
export function DashboardHeader({ email }: DashboardHeaderProps) {
  const name = displayNameFromEmail(email);
  const heading = name ? `${greetingForHour()}, ${name}` : "Welcome back";

  return (
    <header className="mb-8">
      <h1 className="font-display text-3xl leading-tight text-ink">
        {heading}
      </h1>
      <p className="mt-1.5 text-sm text-muted">
        Turn your resume into an evidence-backed match.
      </p>
    </header>
  );
}
