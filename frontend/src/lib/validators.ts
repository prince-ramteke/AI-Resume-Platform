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

/** Resume upload limit — mirrors backend FileValidator.MAX_SIZE_BYTES (10 MB). */
export const RESUME_MAX_BYTES = 10 * 1024 * 1024;

/** Accepted resume extensions — mirrors backend FileValidator.ALLOWED_EXTENSIONS. */
export const RESUME_ACCEPT_EXTENSIONS = [".pdf", ".docx"] as const;

/**
 * Resume file: mirrors backend FileValidator (PDF/DOCX, ≤10 MB). UX-only —
 * the server still validates content-type and magic bytes authoritatively.
 * We check extension + size here for immediate feedback; content sniffing is
 * intentionally left to the backend. Returns a message, or undefined if valid.
 */
export function validateResumeFile(file: File | null): string | undefined {
  if (!file) return "Choose a PDF or DOCX file.";
  const name = file.name.toLowerCase();
  const hasAllowedExt = RESUME_ACCEPT_EXTENSIONS.some((ext) => name.endsWith(ext));
  if (!hasAllowedExt) return "File must be a PDF or DOCX.";
  if (file.size === 0) return "That file is empty.";
  if (file.size > RESUME_MAX_BYTES) return "File is larger than 10 MB.";
  return undefined;
}
