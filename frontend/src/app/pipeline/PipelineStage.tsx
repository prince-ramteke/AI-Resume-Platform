import { ScanBeam } from "./ScanBeam";

export type StageState = "idle" | "active" | "done" | "fail";

interface Props {
  index: number;
  total: number;
  label: string;
  state?: StageState;
  size?: "default" | "hero";
}

function StageIcon({ state }: { state: StageState }) {
  if (state === "done") {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    );
  }
  if (state === "fail") {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M18 6 6 18" />
        <path d="m6 6 12 12" />
      </svg>
    );
  }
  if (state === "active") {
    return (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
        <circle cx="12" cy="12" r="8" />
        <circle cx="12" cy="12" r="3" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
    </svg>
  );
}

export function PipelineStage({ index, total, label, state = "idle", size = "default" }: Props) {
  return (
    <div className="app-pipeline__stage">
      <div className="app-pipeline__tile" data-state={state === "idle" ? undefined : state} data-size={size === "hero" ? "hero" : undefined}>
        <span className="app-pipeline__idx">
          {String(index).padStart(2, "0")}/{String(total).padStart(2, "0")}
        </span>
        <StageIcon state={state} />
        <span className="app-pipeline__dot" />
        {state === "active" && <ScanBeam mode="horizontal" />}
      </div>
      <span className="app-pipeline__label">{label}</span>
    </div>
  );
}
