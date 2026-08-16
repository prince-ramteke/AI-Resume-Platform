import React from "react";
import { Button } from "./Button";

interface Props {
  title?: string;
  body?: React.ReactNode;
  code?: string;
  onRetry?: () => void;
  retryLabel?: string;
  onReport?: () => void;
}

function TornPage() {
  return (
    <svg width="64" height="80" viewBox="0 0 64 80" aria-hidden="true">
      <path
        d="M4 4 L52 4 L60 12 L60 60 L54 66 L46 60 L36 68 L26 60 L18 66 L10 60 L4 66 Z"
        fill="var(--paper-100)"
        stroke="var(--signal-fail)"
        strokeWidth="1"
      />
      <line x1="14" y1="20" x2="46" y2="20" stroke="var(--paper-ink-2)" strokeOpacity=".3" />
      <line x1="14" y1="28" x2="40" y2="28" stroke="var(--paper-ink-2)" strokeOpacity=".22" />
      <line x1="14" y1="36" x2="44" y2="36" stroke="var(--paper-ink-2)" strokeOpacity=".22" />
      <path d="M4 66 L18 74 L28 66 L38 74 L48 66 L60 74"
        fill="none" stroke="var(--signal-fail)" strokeWidth="1.5" />
    </svg>
  );
}

export function ErrorState({
  title = "Something didn't parse.",
  body = "That request didn't complete. Try again or report the issue.",
  code,
  onRetry,
  retryLabel = "Try again",
  onReport,
}: Props) {
  return (
    <div className="app-empty" role="alert">
      <TornPage />
      <h2 className="app-empty__title" style={{ fontFamily: "var(--font-mono)", fontSize: 22, letterSpacing: ".02em" }}>
        {title}
      </h2>
      {body && <p className="app-empty__body">{body}</p>}
      {code && <span className="app-error-code">ERR — {code}</span>}
      {(onRetry || onReport) && (
        <div style={{ display: "flex", gap: 12 }}>
          {onRetry && <Button variant="primary" onClick={onRetry}>{retryLabel}</Button>}
          {onReport && <Button variant="ghost" onClick={onReport}>Report</Button>}
        </div>
      )}
    </div>
  );
}
