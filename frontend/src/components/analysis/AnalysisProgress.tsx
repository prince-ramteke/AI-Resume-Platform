import { useEffect, useState } from "react";
import { Button, Card, Spinner } from "../ui";

interface AnalysisProgressProps {
  /** True while the mutation is running (drives timer + stage cycling). */
  isRunning: boolean;
  /** Called when the user chooses to stop waiting on the client side. */
  onStopWaiting: () => void;
}

/** Cosmetic pacing stages — NOT backend events. Cycled on a fixed interval. */
const STAGES = [
  "Reading your resume",
  "Reading the job description",
  "Retrieving relevant passages",
  "Scoring the match",
  "Finalizing recommendations",
] as const;

const STAGE_INTERVAL_MS = 15_000;
const TICK_MS = 1000;

function formatElapsed(seconds: number): string {
  const m = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const s = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");
  return `${m}:${s}`;
}

/**
 * The long-running analysis card. Real measured latency on the local Ollama
 * stack is ~2 minutes, so this state must feel intentional, not stuck.
 *
 * The staged label is honest: cosmetic pacing tied to a client-side interval,
 * NOT progress reported by the backend (there is none — the API is one
 * synchronous POST). We say so on the card so users don't infer more than we
 * actually know.
 *
 * "Stop waiting" only aborts the client-side wait. The backend request keeps
 * running and may still persist a row; the button copy and follow-up message
 * reflect that.
 */
export function AnalysisProgress({
  isRunning,
  onStopWaiting,
}: AnalysisProgressProps) {
  const [elapsed, setElapsed] = useState(0);
  const [stageIdx, setStageIdx] = useState(0);

  useEffect(() => {
    if (!isRunning) return;
    setElapsed(0);
    setStageIdx(0);
    const tick = setInterval(() => setElapsed((e) => e + 1), TICK_MS);
    const stage = setInterval(
      () => setStageIdx((i) => Math.min(i + 1, STAGES.length - 1)),
      STAGE_INTERVAL_MS
    );
    return () => {
      clearInterval(tick);
      clearInterval(stage);
    };
  }, [isRunning]);

  return (
    <Card>
      <div className="flex flex-col items-center gap-3 py-2 text-center">
        <Spinner />
        <p className="font-display text-lg text-ink">
          Analyzing your resume against the job description…
        </p>
        {/*
          A11y: the elapsed timer ticks every second, so it is deliberately
          NOT inside a live region — a re-announced counter is worse than
          silence for screen-reader users. The stage label (below) IS the
          spoken progress signal, and only changes every ~15 s.
        */}
        <p
          className="font-mono text-sm text-muted"
          aria-hidden="true"
        >
          {formatElapsed(elapsed)}
        </p>
        <p
          role="status"
          aria-live="polite"
          aria-atomic="true"
          className="text-sm text-ink"
        >
          {STAGES[stageIdx]}
        </p>
        <p className="max-w-md text-xs text-muted">
          This can take up to a couple of minutes on your local Ollama model.
          These stages are frontend pacing — the backend runs the analysis in a
          single call and doesn't report progress.
        </p>
        <p
          role="alert"
          className="mt-1 rounded-control border border-warning/40 bg-warning-soft px-3 py-2 text-xs text-ink"
        >
          Don't refresh — this analysis will be lost.
        </p>
        <div className="mt-2">
          <Button variant="secondary" onClick={onStopWaiting}>
            Stop waiting
          </Button>
        </div>
      </div>
    </Card>
  );
}
