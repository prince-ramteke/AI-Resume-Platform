import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useResumes } from "../../hooks/useResumes";
import { useJobDescriptions } from "../../hooks/useJobDescriptions";
import { useRunAnalysis } from "../../hooks/useRunAnalysis";
import { useBeforeUnload } from "../../hooks/useBeforeUnload";
import { PageHeader } from "../primitives/PageHeader";
import { Card } from "../primitives/Card";
import { Button } from "../primitives/Button";
import { Eyebrow } from "../primitives/Eyebrow";
import { ErrorState } from "../primitives/ErrorState";
import { Select } from "../primitives/Select";
import { PipelineTrack, PIPELINE_STAGES } from "../pipeline/PipelineTrack";
import { DocumentTheatre } from "../processing/DocumentTheatre";
import { useStageClock } from "../processing/useStageClock";
import { usePrefersReducedMotion } from "../useTheme";
import { parseApiError } from "../../api/errors";

/**
 * Both the picker view AND the processing theatre live here so that starting
 * an analysis is a single call to useRunAnalysis without needing a new backend
 * polling endpoint. The mutation resolves after ~2 min; while pending we run
 * the stage animation locally; on success we redirect to /analyses/:id.
 */
export function NewAnalysisPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const resumes = useResumes({ page: 0, size: 100, sort: "createdAt,desc" });
  const jobs = useJobDescriptions({ page: 0, size: 100, sort: "createdAt,desc" });

  const [resumeId, setResumeId] = useState<number | null>(() => {
    const q = Number(params.get("resume"));
    return Number.isFinite(q) && q > 0 ? q : null;
  });
  const [jobId, setJobId] = useState<number | null>(() => {
    const q = Number(params.get("job"));
    return Number.isFinite(q) && q > 0 ? q : null;
  });

  const run = useRunAnalysis();
  const reduced = usePrefersReducedMotion();
  const clock = useStageClock(run.isPending, reduced);
  const abortRef = useRef<AbortController | null>(null);
  const logContainerRef = useRef<HTMLDivElement | null>(null);

  useBeforeUnload(run.isPending);

  useEffect(() => {
    if (run.isSuccess && run.data) navigate(`/analyses/${run.data.id}`, { replace: true });
  }, [run.isSuccess, run.data, navigate]);

  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTo({ top: logContainerRef.current.scrollHeight, behavior: reduced ? "auto" : "smooth" });
    }
  }, [clock.log.length, reduced]);

  function start() {
    if (!resumeId || !jobId) return;
    const controller = new AbortController();
    abortRef.current = controller;
    run.mutate({ resumeId, jobDescriptionId: jobId, signal: controller.signal });
  }

  function cancel() {
    abortRef.current?.abort();
  }

  const resumeOptions =
    resumes.data?.content.map((r) => ({ label: `#${r.id} · ${r.filename}`, value: String(r.id) })) ?? [];
  const jobOptions =
    jobs.data?.content.map((j) => ({ label: `#${j.id} · ${j.title}`, value: String(j.id) })) ?? [];

  const bothChosen = resumeId != null && jobId != null;

  if (run.isError) {
    const parsed = parseApiError(run.error);
    return (
      <div className="theatre-layout">
        <ErrorState
          title="The analysis didn't finish."
          body={parsed.message}
          code={`analysis.${parsed.status ?? "network"}`}
          onRetry={() => run.reset()}
          retryLabel="Back to picker"
        />
      </div>
    );
  }

  if (run.isPending) {
    return (
      <div className="theatre-layout">
        <div className="theatre-header">
          <Eyebrow>Analysis · in progress</Eyebrow>
          <div className="theatre-caption-wrap">
            <h1 key={clock.caption} className="theatre-caption">
              {clock.caption}
            </h1>
          </div>
          <div
            aria-live="polite"
            style={{ position: "absolute", width: 1, height: 1, padding: 0, margin: -1, overflow: "hidden", clip: "rect(0,0,0,0)", whiteSpace: "nowrap" }}
          >
            Stage {clock.stageIdx} of {PIPELINE_STAGES.length} — {clock.stageName}
          </div>
        </div>

        <div className="theatre-track">
          <PipelineTrack steps={clock.steps} size="hero" />
        </div>

        <DocumentTheatre stageIdx={clock.stageIdx} />

        <div className="theatre-log">
          <Card tone="flat" style={{ padding: 16 }}>
            <Eyebrow>Log</Eyebrow>
            <div
              ref={logContainerRef}
              className="theatre-log-lines"
            >
              {clock.log.length === 0 ? <span>…</span> : clock.log.map((l, i) => {
                const isStageTransition = l.includes("· in progress");
                return (
                  <span key={i} className={isStageTransition ? "theatre-log-stage" : undefined}>
                    {l}
                  </span>
                );
              })}
            </div>
          </Card>
        </div>

        <div className="theatre-actions">
          <Button variant="ghost" onClick={cancel}>Stop waiting</Button>
        </div>
        <div className="theatre-hint">
          A local model can take ~2 minutes. The pipeline runs server-side; stopping only stops the client wait.
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 720, margin: "0 auto", padding: "48px 32px" }}>
      <PageHeader eyebrow="New analysis" title="Match a resume against a role." />

      <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
        <Card>
          <Eyebrow>Step 01 · Resume</Eyebrow>
          <div style={{ marginTop: 16 }}>
            {resumes.isError ? (
              <ErrorState code="fetch.resumes" onRetry={() => void resumes.refetch()} />
            ) : resumeOptions.length === 0 ? (
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 16 }}>
                <span style={{ color: "var(--fg-3)" }}>No resumes uploaded yet.</span>
                <Button to="/resumes?new=1" variant="secondary">Upload resume</Button>
              </div>
            ) : (
              <Select
                label="Resume"
                value={resumeId ? String(resumeId) : ""}
                onChange={(e) => setResumeId(e.target.value ? Number(e.target.value) : null)}
                options={[{ label: "Pick a resume…", value: "" }, ...resumeOptions]}
              />
            )}
          </div>
        </Card>

        <Card>
          <Eyebrow>Step 02 · Job description</Eyebrow>
          <div style={{ marginTop: 16 }}>
            {jobs.isError ? (
              <ErrorState code="fetch.jobDescriptions" onRetry={() => void jobs.refetch()} />
            ) : jobOptions.length === 0 ? (
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 16 }}>
                <span style={{ color: "var(--fg-3)" }}>No job descriptions saved yet.</span>
                <Button to="/job-descriptions" variant="secondary">Add job description</Button>
              </div>
            ) : (
              <Select
                label="Job description"
                value={jobId ? String(jobId) : ""}
                onChange={(e) => setJobId(e.target.value ? Number(e.target.value) : null)}
                options={[{ label: "Pick a JD…", value: "" }, ...jobOptions]}
              />
            )}
          </div>
        </Card>

        <Card tone="stage">
          <Eyebrow>Pipeline preview</Eyebrow>
          <div style={{ marginTop: 24 }}>
            <PipelineTrack
              steps={PIPELINE_STAGES.map((label, i) => ({
                label,
                state: bothChosen && i === 0 ? "active" : "idle",
              }))}
            />
          </div>
        </Card>

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 12 }}>
          <Button variant="ghost" to="/dashboard">Cancel</Button>
          <Button variant="primary" disabled={!bothChosen} onClick={start}>
            Run analysis →
          </Button>
        </div>
      </div>
    </div>
  );
}
