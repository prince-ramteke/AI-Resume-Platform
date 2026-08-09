import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Alert,
  Button,
  EmptyState,
  ErrorState,
  LinkButton,
  Pagination,
  Skeleton,
} from "../../components/ui";
import { AnalysisIcon, ResumeIcon, JobIcon } from "../../components/layout/icons";
import { ResumePickerRow } from "../../components/analysis/ResumePickerRow";
import { JobDescriptionPickerRow } from "../../components/analysis/JobDescriptionPickerRow";
import { AnalysisProgress } from "../../components/analysis/AnalysisProgress";
import { useResumes } from "../../hooks/useResumes";
import { useJobDescriptions } from "../../hooks/useJobDescriptions";
import { useRunAnalysis } from "../../hooks/useRunAnalysis";
import { useBeforeUnload } from "../../hooks/useBeforeUnload";
import { parseApiError } from "../../api/errors";

const PICKER_PAGE_SIZE = 5;

function PickerSkeleton() {
  return (
    <div role="status" aria-label="Loading options" className="flex flex-col gap-2">
      {[0, 1, 2].map((i) => (
        <Skeleton key={i} className="h-14 w-full rounded-control" />
      ))}
    </div>
  );
}

/**
 * Pick a resume + a JD, then submit. Both selections seed the URL so a
 * deep-link (`?resumeId=&jobDescriptionId=`) can pre-fill the pickers — the
 * dashboard/detail-page CTAs use this. The pickers are paginated (page-size
 * 5) but do NOT invent a search endpoint: neither the resume nor JD list has
 * server-side title search wired here, matching the API contract.
 *
 * The submit-and-wait path is the whole reason this page is careful about
 * state: while `isPending`, inputs are locked, the beforeunload guard is
 * armed, and a client-side AbortController lets the user cut the wait. On
 * success we seed the detail cache from the returned body, invalidate the
 * list, then navigate — the mutation's onSuccess handles the cache work.
 */
export function NewAnalysisPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Server queries for the two pickers, paginated independently.
  const [resumePage, setResumePage] = useState(0);
  const [jdPage, setJdPage] = useState(0);
  const resumesQ = useResumes({
    page: resumePage,
    size: PICKER_PAGE_SIZE,
    sort: "createdAt,desc",
  });
  const jdsQ = useJobDescriptions({
    page: jdPage,
    size: PICKER_PAGE_SIZE,
    sort: "createdAt,desc",
  });

  // Selection state seeded from URL params so deep-links pre-fill.
  const initialResumeId = parsePositiveInt(searchParams.get("resumeId"));
  const initialJdId = parsePositiveInt(searchParams.get("jobDescriptionId"));
  const [selectedResumeId, setSelectedResumeId] = useState<number | null>(
    initialResumeId
  );
  const [selectedJdId, setSelectedJdId] = useState<number | null>(initialJdId);

  // Keep the URL in sync so a share/refresh restores the selection (subject
  // to memory-only JWT). Only writes when a picked value changes — never on
  // page load — so we don't stomp other query params.
  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (selectedResumeId !== null) params.set("resumeId", String(selectedResumeId));
    else params.delete("resumeId");
    if (selectedJdId !== null)
      params.set("jobDescriptionId", String(selectedJdId));
    else params.delete("jobDescriptionId");
    // Only call setSearchParams if the string actually changed (avoids loops).
    if (params.toString() !== searchParams.toString()) {
      setSearchParams(params, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedResumeId, selectedJdId]);

  // Mutation + client-side abort controller for "Stop waiting".
  const run = useRunAnalysis();
  const abortRef = useRef<AbortController | null>(null);
  const [stoppedWaiting, setStoppedWaiting] = useState(false);

  useBeforeUnload(run.isPending);

  function handleSubmit() {
    if (!selectedResumeId || !selectedJdId || run.isPending) return;
    setStoppedWaiting(false);
    const controller = new AbortController();
    abortRef.current = controller;
    run.mutate(
      {
        resumeId: selectedResumeId,
        jobDescriptionId: selectedJdId,
        signal: controller.signal,
      },
      {
        onSuccess: (result) => {
          navigate(`/analyses/${result.id}?fromRun=1`, { replace: true });
        },
      }
    );
  }

  function handleStopWaiting() {
    abortRef.current?.abort();
    abortRef.current = null;
    setStoppedWaiting(true);
    run.reset();
  }

  const totalResumes = resumesQ.data?.totalElements ?? 0;
  const totalJds = jdsQ.data?.totalElements ?? 0;

  // First-time empty state: nothing to pick from anywhere.
  const bothEmpty =
    resumesQ.isSuccess &&
    jdsQ.isSuccess &&
    totalResumes === 0 &&
    totalJds === 0;

  const runError = useMemo(() => {
    if (!run.isError) return null;
    const parsed = parseApiError(run.error);
    // Map a few statuses to more actionable copy than the default banner.
    if (parsed.status === 404) {
      return "One of the selected items is no longer available. Refresh the lists and try again.";
    }
    if (parsed.status === 422) {
      return "We couldn't produce a reliable result from the model. Try again in a moment.";
    }
    // Axios timeout / abort / offline path — no HTTP status.
    if (parsed.status === undefined) {
      return "The analysis took too long or the network dropped. Try again.";
    }
    return parsed.message;
  }, [run.isError, run.error]);

  if (bothEmpty) {
    return (
      <div>
        <PageHeader title="New analysis" back={{ to: "/dashboard", label: "Dashboard" }} />
        <EmptyState
          title="Add a resume and a job description first"
          description="You need at least one resume and one job description before you can run an analysis."
          action={
            <div className="flex flex-wrap gap-2">
              <LinkButton to="/resumes">Add a resume</LinkButton>
              <LinkButton to="/job-descriptions" variant="secondary">
                Add a job description
              </LinkButton>
            </div>
          }
        />
      </div>
    );
  }

  if (run.isPending) {
    return (
      <div>
        <PageHeader
          title="Analyzing…"
          back={{ to: "/dashboard", label: "Dashboard" }}
        />
        <AnalysisProgress isRunning onStopWaiting={handleStopWaiting} />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="New analysis"
        description="Score one of your resumes against a saved job description."
        back={{ to: "/dashboard", label: "Dashboard" }}
      />

      {stoppedWaiting && (
        <div className="mb-6">
          <Alert tone="warning">
            You're no longer waiting for this analysis. It may still finish in
            the background and appear in your history.
          </Alert>
        </div>
      )}

      {runError && (
        <div className="mb-6">
          <Alert tone="error">{runError}</Alert>
        </div>
      )}

      <div className="grid gap-6 md:grid-cols-2">
        <PickerCard
          title="Choose a resume"
          icon={<ResumeIcon />}
          isPending={resumesQ.isPending}
          isError={resumesQ.isError}
          onRetry={() => resumesQ.refetch()}
          isFetching={resumesQ.isFetching}
          totalElements={totalResumes}
          emptyTitle="No resumes yet"
          emptyDescription="Upload a resume before running an analysis."
          emptyAction={<LinkButton to="/resumes">Upload a resume</LinkButton>}
        >
          <div role="radiogroup" aria-label="Select a resume" className="flex flex-col gap-2">
            {(resumesQ.data?.content ?? []).map((r) => (
              <ResumePickerRow
                key={r.id}
                resume={r}
                selected={selectedResumeId === r.id}
                onSelect={setSelectedResumeId}
              />
            ))}
          </div>
          {resumesQ.data && (
            <Pagination
              page={resumePage}
              totalPages={resumesQ.data.totalPages}
              totalElements={totalResumes}
              pageSize={PICKER_PAGE_SIZE}
              onPageChange={setResumePage}
              isFetching={resumesQ.isFetching}
            />
          )}
        </PickerCard>

        <PickerCard
          title="Choose a job description"
          icon={<JobIcon />}
          isPending={jdsQ.isPending}
          isError={jdsQ.isError}
          onRetry={() => jdsQ.refetch()}
          isFetching={jdsQ.isFetching}
          totalElements={totalJds}
          emptyTitle="No job descriptions yet"
          emptyDescription="Add a job description before running an analysis."
          emptyAction={
            <LinkButton to="/job-descriptions">Add a job description</LinkButton>
          }
        >
          <div role="radiogroup" aria-label="Select a job description" className="flex flex-col gap-2">
            {(jdsQ.data?.content ?? []).map((jd) => (
              <JobDescriptionPickerRow
                key={jd.id}
                jobDescription={jd}
                selected={selectedJdId === jd.id}
                onSelect={setSelectedJdId}
              />
            ))}
          </div>
          {jdsQ.data && (
            <Pagination
              page={jdPage}
              totalPages={jdsQ.data.totalPages}
              totalElements={totalJds}
              pageSize={PICKER_PAGE_SIZE}
              onPageChange={setJdPage}
              isFetching={jdsQ.isFetching}
            />
          )}
        </PickerCard>
      </div>

      <div className="mt-8 flex flex-wrap items-center justify-end gap-3">
        <p className="text-xs text-muted">
          Analyses can take up to a couple of minutes on the local model.
        </p>
        <Button
          leftIcon={<AnalysisIcon className="h-4 w-4" />}
          onClick={handleSubmit}
          disabled={!selectedResumeId || !selectedJdId}
        >
          Run analysis
        </Button>
      </div>
    </div>
  );
}

interface PickerCardProps {
  title: string;
  icon: React.ReactNode;
  isPending: boolean;
  isError: boolean;
  onRetry: () => void;
  isFetching: boolean;
  totalElements: number;
  emptyTitle: string;
  emptyDescription: string;
  emptyAction: React.ReactNode;
  children: React.ReactNode;
}

function PickerCard({
  title,
  icon,
  isPending,
  isError,
  onRetry,
  isFetching,
  totalElements,
  emptyTitle,
  emptyDescription,
  emptyAction,
  children,
}: PickerCardProps) {
  return (
    <section
      aria-label={title}
      className="flex flex-col gap-3 rounded-card border border-border bg-surface p-4"
    >
      <div className="flex items-center gap-2">
        <span className="shrink-0 text-accent" aria-hidden="true">
          {icon}
        </span>
        <h2 className="font-display text-lg text-ink">{title}</h2>
      </div>
      {isPending ? (
        <PickerSkeleton />
      ) : isError ? (
        <ErrorState
          title="Couldn't load the list"
          message="Try again in a moment."
          onRetry={onRetry}
        />
      ) : totalElements === 0 ? (
        <EmptyState
          title={emptyTitle}
          description={emptyDescription}
          action={emptyAction}
        />
      ) : (
        <div className={isFetching ? "opacity-60 transition-opacity" : ""}>
          {children}
        </div>
      )}
    </section>
  );
}

function parsePositiveInt(value: string | null): number | null {
  if (!value) return null;
  const n = Number.parseInt(value, 10);
  return Number.isInteger(n) && n > 0 ? n : null;
}
