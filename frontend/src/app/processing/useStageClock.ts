import { useEffect, useMemo, useState } from "react";
import { PIPELINE_STAGES } from "../pipeline/PipelineTrack";
import type { StageState } from "../pipeline/PipelineStage";

export interface StageClock {
  stageIdx: number;
  stageName: string;
  caption: string;
  steps: Array<{ label: string; state: StageState }>;
  log: string[];
}

const CAPTIONS: string[] = [
  "Reading the resume.",
  "Extracting structured content.",
  "Splitting into semantic units.",
  "Generating vectors.",
  "Finding evidence in the job description.",
  "Weighing evidence.",
  "Composing analysis.",
];

/**
 * Drives the pipeline stage animation while the analysis mutation is pending.
 * No fake percentages — stages advance on a fixed cadence to communicate
 * activity. If the backend finishes sooner, the caller redirects.
 */
export function useStageClock(isPending: boolean, reducedMotion: boolean): StageClock {
  const [stageIdx, setStageIdx] = useState(0);
  const [log, setLog] = useState<string[]>([]);

  useEffect(() => {
    if (!isPending) {
      setStageIdx(0);
      setLog([]);
      return;
    }
    setStageIdx(1);
    setLog(["t+0.0s · pipeline · started"]);
    if (reducedMotion) return;

    const stageMs = 26000;
    const interval = window.setInterval(() => {
      setStageIdx((s) => {
        const next = s < PIPELINE_STAGES.length ? s + 1 : s;
        const label = (PIPELINE_STAGES[next - 1] ?? "…").toLowerCase();
        setLog((prev) => [
          ...prev.slice(-5),
          `t+${((next * stageMs) / 1000).toFixed(1)}s · ${label} · in progress`,
        ]);
        return next;
      });
    }, stageMs);
    return () => window.clearInterval(interval);
  }, [isPending, reducedMotion]);

  const steps = useMemo(
    () =>
      PIPELINE_STAGES.map((label, i) => ({
        label,
        state: (i < stageIdx - 1
          ? "done"
          : i === stageIdx - 1
            ? "active"
            : "idle") as StageState,
      })),
    [stageIdx],
  );

  const clampedIdx = Math.max(0, Math.min(PIPELINE_STAGES.length - 1, stageIdx - 1));

  return {
    stageIdx,
    stageName: PIPELINE_STAGES[clampedIdx] ?? "",
    caption: CAPTIONS[clampedIdx] ?? "",
    steps,
    log,
  };
}
