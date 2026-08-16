import { createContext, useCallback, useContext, useEffect, useState } from "react";

export type AppTheme = "light" | "dark";

const STORAGE_KEY = "ri-app-theme";
const DOC_ATTR = "data-app-theme";

function readInitial(): AppTheme {
  if (typeof window === "undefined") return "light";
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "light" || stored === "dark") return stored;
  } catch {
    /* localStorage might be disabled — fall through */
  }
  if (typeof document !== "undefined") {
    const attr = document.documentElement.getAttribute(DOC_ATTR);
    if (attr === "light" || attr === "dark") return attr;
  }
  return "light";
}

interface ThemeContextValue {
  theme: AppTheme;
  setTheme: (t: AppTheme) => void;
  toggle: () => void;
}

export const ThemeCtx = createContext<ThemeContextValue | null>(null);

/**
 * Owns the single theme state instance. Called once in AppRoot, which
 * provides the return value via ThemeCtx.
 */
export function useThemeState(): ThemeContextValue {
  const [theme, setThemeState] = useState<AppTheme>(readInitial);

  const setTheme = useCallback((next: AppTheme) => {
    setThemeState(next);
  }, []);

  const toggle = useCallback(() => {
    setThemeState(prev => prev === "dark" ? "light" : "dark");
  }, []);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* ignore quota / disabled storage */
    }
    if (typeof document !== "undefined") {
      document.documentElement.setAttribute(DOC_ATTR, theme);
    }
  }, [theme]);

  return { theme, setTheme, toggle };
}

/**
 * Reads the shared theme from context. Every consumer sees the same state.
 */
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeCtx);
  if (!ctx) throw new Error("useTheme requires ThemeCtx — wrap the tree in AppRoot");
  return ctx;
}

export function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState<boolean>(() => {
    if (typeof window === "undefined" || !window.matchMedia) return false;
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  });
  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) return;
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    const handler = (e: MediaQueryListEvent) => setReduced(e.matches);
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, []);
  return reduced;
}
