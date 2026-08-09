import { useEffect } from "react";

/**
 * When `enabled` is true, prompts the user with the browser's native "leave
 * this page?" dialog on tab close / refresh. Used during the ~2-minute
 * analysis run so a refresh doesn't silently lose the pending result (the
 * JWT is memory-only and there is no server-side job to reattach to).
 *
 * Modern browsers ignore the returned string and show their own copy; we
 * still set `returnValue` for legacy compatibility.
 */
export function useBeforeUnload(enabled: boolean): void {
  useEffect(() => {
    if (!enabled) return;
    function handler(e: BeforeUnloadEvent) {
      e.preventDefault();
      e.returnValue = "";
    }
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [enabled]);
}
