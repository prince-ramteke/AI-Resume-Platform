import { toneForScore } from "../../lib/scoreTone";
import type { ScoreTone } from "../../lib/scoreTone";

interface ScoreGaugeProps {
  /** Match score, 0–100 (backend guarantees integer). */
  score: number;
  /** Diameter in px; the SVG remains fluid via viewBox. Defaults to 176. */
  size?: number;
}

const TONE_STROKE: Record<ScoreTone, string> = {
  success: "text-success",
  warning: "text-warning",
  danger: "text-danger",
};

/**
 * Semicircle gauge for the match score. Pure inline SVG — no chart library.
 * The arc length is driven by stroke-dashoffset so the "fill" grows smoothly
 * with the score value; the underlying track stays visible for context.
 *
 * A11y: `role="img"` + a full-sentence `aria-label`. The visible number is
 * decorative to the screen reader (`aria-hidden`) so the label isn't read
 * twice.
 */
export function ScoreGauge({ score, size = 176 }: ScoreGaugeProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(score)));
  const tone = toneForScore(clamped);
  const strokeClass = TONE_STROKE[tone];

  // Semicircle math: viewBox 200x110, radius 90, stroke width 16.
  // Circumference of the half-arc is π*r; dashoffset shrinks with score.
  const radius = 90;
  const circumference = Math.PI * radius;
  const offset = circumference * (1 - clamped / 100);

  return (
    <div
      role="img"
      aria-label={`Match score ${clamped} out of 100`}
      className="flex flex-col items-center"
    >
      <svg
        viewBox="0 0 200 110"
        width={size}
        height={(size * 110) / 200}
        className="max-w-full"
      >
        {/* Track */}
        <path
          d="M 10 100 A 90 90 0 0 1 190 100"
          fill="none"
          stroke="currentColor"
          strokeWidth="16"
          strokeLinecap="round"
          className="text-border"
        />
        {/* Value */}
        <path
          d="M 10 100 A 90 90 0 0 1 190 100"
          fill="none"
          stroke="currentColor"
          strokeWidth="16"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className={`${strokeClass} transition-[stroke-dashoffset] duration-700 ease-out`}
        />
        <text
          x="100"
          y="88"
          textAnchor="middle"
          className="fill-ink font-display"
          style={{ fontSize: "44px", fontWeight: 600 }}
          aria-hidden="true"
        >
          {clamped}
        </text>
        <text
          x="100"
          y="106"
          textAnchor="middle"
          className="fill-muted"
          style={{ fontSize: "11px", letterSpacing: "0.08em" }}
          aria-hidden="true"
        >
          OUT OF 100
        </text>
      </svg>
      <p className="mt-2 text-xs text-muted">Higher is a stronger match.</p>
    </div>
  );
}
