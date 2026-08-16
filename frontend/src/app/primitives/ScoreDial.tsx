import { useEffect, useState } from "react";
import { usePrefersReducedMotion } from "../useTheme";

interface Props {
  /** Percentage value 0–100. */
  value: number;
  size?: number;
  label?: string;
  sublabel?: string;
}

/**
 * Amber arc sweep — evidence voice, not machine voice. 900ms ease-entrance.
 * Uses stroke-dashoffset animation on an SVG circle.
 */
export function ScoreDial({ value, size = 120, label = "match strength", sublabel }: Props) {
  const reduced = usePrefersReducedMotion();
  const [shown, setShown] = useState<number>(reduced ? value : 0);

  useEffect(() => {
    if (reduced) { setShown(value); return; }
    let raf = 0;
    let start = 0;
    const dur = 900;
    // ease-entrance: cubic-bezier(.22,1,.36,1) — approximate via a similar cubic.
    const ease = (t: number) => 1 - Math.pow(1 - t, 3);
    const step = (ts: number) => {
      if (!start) start = ts;
      const p = Math.min(1, (ts - start) / dur);
      setShown(value * ease(p));
      if (p < 1) raf = window.requestAnimationFrame(step);
    };
    raf = window.requestAnimationFrame(step);
    return () => window.cancelAnimationFrame(raf);
  }, [value, reduced]);

  const stroke = 3;
  const r = size / 2 - stroke * 2;
  const C = 2 * Math.PI * r;
  const pct = Math.max(0, Math.min(100, shown)) / 100;
  const num = Math.round(shown);
  const numSize = Math.round(size * 0.28);

  return (
    <div className="app-dial" style={{ width: size }}>
      <div style={{ position: "relative", width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ transform: "rotate(-90deg)" }} aria-hidden="true">
          <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--line-2)" strokeWidth={stroke} />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--amber-500)"
            strokeWidth={stroke}
            strokeLinecap="round"
            strokeDasharray={C}
            strokeDashoffset={C * (1 - pct)}
          />
        </svg>
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <span className="app-dial__num" style={{ fontSize: numSize }}>{num}</span>
        </div>
      </div>
      <span className="app-dial__label" role="img" aria-label={`${num} out of 100`}>{label}</span>
      {sublabel && <span style={{ fontSize: 12, color: "var(--fg-4)" }}>{sublabel}</span>}
    </div>
  );
}
