/**
 * Client-side validators — UX only. The backend Bean Validation constraints
 * (see backend auth DTOs and docs/API.md §7) remain authoritative; these mirror
 * them so users get immediate feedback without a round-trip. Pure functions:
 * each returns an error message, or `undefined` when the value is valid.
 */

// Pragmatic email shape check (not RFC-exhaustive — the backend's @Email is the
// source of truth). Rejects the obvious cases: no @, no domain, whitespace.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Required + valid email format (login and register). */
export function validateEmail(value: string): string | undefined {
  const v = value.trim();
  if (!v) return "Enter your email.";
  if (!EMAIL_RE.test(v)) return "Enter a valid email address.";
  return undefined;
}

/** Login password: required only. Existing accounts aren't re-checked for composition. */
export function validateLoginPassword(value: string): string | undefined {
  if (!value) return "Enter your password.";
  return undefined;
}

/**
 * Register password: mirrors backend RegisterRequest — min 8 chars, at least one
 * letter, at least one digit. Not trimmed (spaces may be intentional).
 */
export function validatePassword(value: string): string | undefined {
  if (!value) return "Enter a password.";
  if (value.length < 8) return "Use at least 8 characters.";
  if (!/[a-zA-Z]/.test(value)) return "Include at least one letter.";
  if (!/\d/.test(value)) return "Include at least one number.";
  return undefined;
}

/** Confirm password: required + must match the password field. */
export function validateConfirm(
  password: string,
  confirm: string
): string | undefined {
  if (!confirm) return "Re-enter your password.";
  if (confirm !== password) return "Passwords don't match.";
  return undefined;
}
