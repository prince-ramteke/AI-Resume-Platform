import { Link } from "react-router-dom";
import { Badge } from "../ui";
import { formatRelativeTime } from "../../lib/formatDate";
import { toneForScore } from "../../lib/scoreTone";
import type { AnalysisSummary } from "../../types/analysis";

interface RecentAnalysisRowProps {
  analysis: AnalysisSummary;
}

/**
 * One recent-analysis row. Links to the analysis detail route (which ships in
 * M6.6). The score is a status-toned mono badge with a screen-reader label so
 * the bare number isn't read out of context.
 */
export function RecentAnalysisRow({ analysis }: RecentAnalysisRowProps) {
  const { id, score, jobTitle, createdAt } = analysis;

  return (
    <Link
      to={`/analyses/${id}`}
      className="flex items-center gap-4 rounded-control px-3 py-3 transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
    >
      <Badge tone={toneForScore(score)} mono>
        <span aria-hidden="true">{score}</span>
        <span className="sr-only">Match score {score} out of 100</span>
      </Badge>
      <span className="min-w-0 flex-1 truncate text-sm font-medium text-ink">
        {jobTitle}
      </span>
      <span className="shrink-0 text-xs text-muted">
        {formatRelativeTime(createdAt)}
      </span>
    </Link>
  );
}
