import React from "react";
import { useTheme, useThemeState, ThemeCtx } from "./useTheme";

interface AppRootProps {
  children: React.ReactNode;
}

function AppRootShell({ children }: { children: React.ReactNode }) {
  const { theme } = useTheme();
  return (
    <div className="app-root" data-theme={theme}>
      <AppErrorBoundary>{children}</AppErrorBoundary>
    </div>
  );
}

/**
 * Scoped wrapper for every authenticated-app surface. Owns the single
 * theme state and provides it via ThemeCtx so every useTheme() consumer
 * (AppRootShell, ThemeToggle, etc.) shares one source of truth.
 */
export function AppRoot({ children }: AppRootProps) {
  const themeValue = useThemeState();
  return (
    <ThemeCtx value={themeValue}>
      <AppRootShell>{children}</AppRootShell>
    </ThemeCtx>
  );
}

interface BoundaryState { hasError: boolean }

class AppErrorBoundary extends React.Component<
  { children: React.ReactNode },
  BoundaryState
> {
  state: BoundaryState = { hasError: false };
  static getDerivedStateFromError(): BoundaryState { return { hasError: true }; }
  componentDidCatch(error: unknown): void {
    // Keep the console signal — never log tokens or user data.
    // eslint-disable-next-line no-console
    console.error("[app] render boundary caught:", error);
  }
  reset = () => { this.setState({ hasError: false }); };
  render() {
    if (this.state.hasError) {
      return (
        <main className="app-main" data-shell="none">
          <div className="app-empty">
            <span className="app-eyebrow">Analysis · unavailable</span>
            <h1 className="app-empty__title">Something didn't render.</h1>
            <p className="app-empty__body">
              An unexpected error stopped the last screen. Reload to try again.
            </p>
            <span className="app-error-code">ERR — app.boundary</span>
            <button
              type="button"
              className="app-btn"
              data-variant="primary"
              onClick={() => window.location.reload()}
            >
              Reload
            </button>
          </div>
        </main>
      );
    }
    return this.props.children;
  }
}
