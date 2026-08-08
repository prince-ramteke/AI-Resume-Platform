import { Link } from "react-router-dom";
import { Button, EmptyState, ErrorState, Skeleton } from "../ui";
import { RecentAnalysisRow } from "./RecentAnalysisRow";
import type { AnalysisSummary } from "../../types/analysis";

interface RecentAnalysesProps {
  analyses: AnalysisSummary[] | undefined;
  /** Query is loading (use React Query's `isPending`, stable across retries). */
  isPending: boolean;
  isError: boolean;
  onRetry: () => void;
}

function RowSkeleton() {
  return (
    <div className="flex items-center gap-4 px-3 py-3">
      <Skeleton className="h-5 w-9 rounded-full" />
      <Skeleton className="h-4 flex-1" />
      <Skeleton className="h-3 w-12" />
    </div>
  );
}

/**
 * Recent-analysis feed. Loads independently of the rest of the dashboard and
 * handles all four states (loading / error / empty / list). The empty state is
 * the first-run nudge into the analysis workflow.
 */
export function RecentAnalyses({
  analyses,
  isPending,
  isError,
  onRetry,
}: RecentAnalysesProps) {
  const hasItems = !!analyses && analyses.length > 0;

  return (
    <section aria-labelledby="recent-analyses-heading">
      <div className="mb-4 flex items-center justify-between">
        <h2
          id="recent-analyses-heading"
          className="font-display text-xl text-ink"
        >
          Recent analyses
        </h2>
        {hasItems && (
          <Link
            to="/analyses"
            className="rounded-control text-sm font-medium text-accent hover:text-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          >
            View all
          </Link>
        )}
      </div>

      {isError ? (
        <ErrorState
          title="Couldn't load your recent analyses"
          message="Something went wrong fetching your analysis history."
          onRetry={onRetry}
        />
      ) : isPending ? (
        <div
          role="status"
          aria-label="Loading recent analyses"
          className="rounded-card border border-border bg-surface p-2"
        >
          <RowSkeleton />
          <RowSkeleton />
          <RowSkeleton />
        </div>
      ) : hasItems ? (
        <div className="rounded-card border border-border bg-surface p-2">
          <ul className="divide-y divide-border">
            {analyses!.map((a) => (
              <li key={a.id}>
                <RecentAnalysisRow analysis={a} />
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <EmptyState
          title="No analyses yet"
          description="Score a resume against a job description to see grounded results here."
          action={
            <Link to="/analyses/new" className="rounded-control">
              <Button>Run your first analysis</Button>
            </Link>
          }
        />
      )}
    </section>
  );
}
