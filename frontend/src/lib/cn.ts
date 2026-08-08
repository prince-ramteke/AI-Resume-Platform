/**
 * Tiny className joiner — no dependency (avoids pulling in clsx/classnames).
 * Falsy values are dropped so conditional classes read cleanly at call sites.
 */
export function cn(
  ...classes: Array<string | false | null | undefined>
): string {
  return classes.filter(Boolean).join(" ");
}
