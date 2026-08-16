import React from "react";

interface Props {
  title: string;
  body?: React.ReactNode;
  eyebrow?: string;
  action?: React.ReactNode;
  /** Optional SVG override. Defaults to the folded paper glyph. */
  illustration?: React.ReactNode;
}

/**
 * The default illustration: a folded page with a hairline scan beam. Static in
 * light, gently pulsing in dark (motion respects prefers-reduced-motion via
 * theme.css collapsing durations to 1ms).
 */
function FoldedPage() {
  return (
    <svg width="64" height="80" viewBox="0 0 64 80" aria-hidden="true">
      <defs>
        <linearGradient id="pgrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--paper-100)" />
          <stop offset="100%" stopColor="var(--paper-300)" />
        </linearGradient>
      </defs>
      <rect x="4" y="4" width="56" height="72" rx="2"
        fill="url(#pgrad)" stroke="var(--line-3)" strokeWidth="1" />
      <path d="M46 4 L60 18 L46 18 Z" fill="var(--paper-200)" stroke="var(--line-3)" strokeWidth="1" />
      <line x1="12" y1="30" x2="52" y2="30" stroke="var(--paper-ink-2)" strokeOpacity=".28" />
      <line x1="12" y1="38" x2="44" y2="38" stroke="var(--paper-ink-2)" strokeOpacity=".22" />
      <line x1="12" y1="46" x2="50" y2="46" stroke="var(--paper-ink-2)" strokeOpacity=".22" />
      <line x1="12" y1="54" x2="40" y2="54" stroke="var(--paper-ink-2)" strokeOpacity=".22" />
      <rect x="4" y="60" width="56" height="2" fill="var(--cyan-500)" opacity=".35" />
    </svg>
  );
}

export function Empty({ title, body, eyebrow, action, illustration }: Props) {
  return (
    <div className="app-empty">
      {eyebrow && <span className="app-eyebrow">{eyebrow}</span>}
      <div>{illustration ?? <FoldedPage />}</div>
      <h2 className="app-empty__title">{title}</h2>
      {body && <p className="app-empty__body">{body}</p>}
      {action && <div>{action}</div>}
    </div>
  );
}
