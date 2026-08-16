import React from "react";
import type { StageState } from "./PipelineStage";
import { PipelineStage } from "./PipelineStage";

export interface PipelineStep {
  label: string;
  state?: StageState;
}

interface Props {
  steps: PipelineStep[];
  size?: "default" | "hero";
  className?: string;
}

/**
 * Canonical seven-stage rail: Resume → Parsing → Chunking → Embeddings →
 * Retrieval → Evidence → Analysis. Consumers pass in only the state overlay;
 * the stage list is imported from PIPELINE_STAGES if they want the default.
 */
export const PIPELINE_STAGES: string[] = [
  "Resume",
  "Parsing",
  "Chunking",
  "Embeddings",
  "Retrieval",
  "Evidence",
  "Analysis",
];

export function PipelineTrack({ steps, size = "default", className }: Props) {
  return (
    <div className={`app-pipeline ${className ?? ""}`.trim()} role="list" aria-label="Analysis pipeline">
      {steps.map((s, i) => (
        <React.Fragment key={s.label + i}>
          <div role="listitem" style={{ flex: 1, minWidth: 0 }}>
            <PipelineStage
              index={i + 1}
              total={steps.length}
              label={s.label}
              state={s.state ?? "idle"}
              size={size}
            />
          </div>
          {i < steps.length - 1 && (
            <div
              className="app-pipeline__connector"
              data-state={
                s.state === "done"
                  ? "done"
                  : s.state === "active"
                  ? "active"
                  : undefined
              }
              aria-hidden="true"
            />
          )}
        </React.Fragment>
      ))}
    </div>
  );
}
