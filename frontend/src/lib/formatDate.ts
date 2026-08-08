/**
 * Compact relative-time label for activity rows (e.g. "just now", "3h ago",
 * "2d ago"), falling back to an absolute date for anything older than ~a month.
 * Pure and deterministic given `now`.
 */
export function formatRelativeTime(iso: string, now: Date = new Date()): string {
  const then = new Date(iso);
  const diffMs = now.getTime() - then.getTime();
  if (Number.isNaN(diffMs)) return "";

  const sec = Math.round(diffMs / 1000);
  const min = Math.round(sec / 60);
  const hr = Math.round(min / 60);
  const day = Math.round(hr / 24);

  if (sec < 45) return "just now";
  if (min < 60) return `${min}m ago`;
  if (hr < 24) return `${hr}h ago`;
  if (day < 30) return `${day}d ago`;

  return then.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

/**
 * Time-of-day greeting word ("Good morning/afternoon/evening") from the local
 * hour. Deterministic given `now`.
 */
export function greetingForHour(now: Date = new Date()): string {
  const h = now.getHours();
  if (h < 12) return "Good morning";
  if (h < 18) return "Good afternoon";
  return "Good evening";
}

/**
 * Derive a friendly display name from an email's local-part. Splits on common
 * separators, title-cases the words, and drops trailing digits/noise. Returns
 * null when nothing reasonable can be produced (caller falls back to a neutral
 * greeting). Never invents data — purely derived from the email.
 *
 * "prince@gmail.com"      -> "Prince"
 * "john.doe@example.com"  -> "John Doe"
 * "a_b-c@x.io"            -> "A B C"
 * "12345@x.io"            -> null
 */
export function displayNameFromEmail(email: string | undefined): string | null {
  if (!email) return null;
  const local = email.split("@")[0] ?? "";
  const words = local
    .split(/[.\-_+]+/)
    .map((w) => w.replace(/[^a-zA-Z]/g, ""))
    .filter((w) => w.length > 0);
  if (words.length === 0) return null;

  const label = words
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ")
    .trim();

  return label.length > 0 ? label : null;
}
