import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Badge,
  EmptyState,
  ErrorState,
  LinkButton,
  Pagination,
  Skeleton,
} from "../../components/ui";
import { AnalysisIcon } from "../../components/layout/icons";
import { Link } from "react-router-dom";
import { useAnalyses } from "../../hooks/useAnalyses";
import { toneForScore } from "../../lib/scoreTone";
import { formatRelativeTime } from "../../lib/formatDate";

const PAGE_SIZE = 10;

function ListSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading analyses"
      className="divide-y divide-border"
    >
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="flex items-center gap-4 px-4 py-4">
          <Skeleton className="h-6 w-10 rounded-full" />
          <Skeleton className="h-4 flex-1" />
          <Skeleton className="h-3 w-16" />
        </div>
      ))}
    </div>
  );
}

/**
 * Paginated history of the caller's analyses. Rows link to the result detail;
 * the API has no search/filter, so we deliberately don't invent one. Paging
 * lives in the URL (`?page=`) so it's shareable and survives back-button.
 */
export function AnalysisHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const pageParam = Number.parseInt(searchParams.get("page") ?? "1", 10);
  const page =
    Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0;

  const query = useAnalyses({
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
  });

  function goToPage(zeroBased: number) {
    setSearchParams({ page: String(zeroBased + 1) });
  }

  // Emptied-page recovery (deleting the last row of a page one day…).
  const isSuccess = query.isSuccess;
  const emptiedPage =
    isSuccess &&
    page > 0 &&
    query.data.content.length === 0 &&
    query.data.totalElements > 0;
  useEffect(() => {
    if (emptiedPage) setSearchParams({ page: String(page) });
  }, [emptiedPage, page, setSearchParams]);

  const totalElements = query.data?.totalElements ?? 0;
  const totalPages = query.data?.totalPages ?? 0;
  const analyses = query.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="History"
        description="Every analysis you've run, newest first."
        action={
          <LinkButton
            to="/analyses/new"
            leftIcon={<AnalysisIcon className="h-4 w-4" />}
          >
            New analysis
          </LinkButton>
        }
      />

      {query.isPending ? (
        <div className="rounded-card border border-border bg-surface">
          <ListSkeleton />
        </div>
      ) : query.isError ? (
        <ErrorState
          title="Couldn't load your analysis history"
          message="Something went wrong fetching your analyses."
          onRetry={() => query.refetch()}
        />
      ) : totalElements === 0 ? (
        <EmptyState
          title="No analyses yet"
          description="Score a resume against a job description to see grounded results here."
          action={
            <LinkButton to="/analyses/new">Run your first analysis</LinkButton>
          }
        />
      ) : (
        <>
          <div
            className={`rounded-card border border-border bg-surface ${
              query.isFetching ? "opacity-60 transition-opacity" : ""
            }`}
          >
            <ul className="divide-y divide-border">
              {analyses.map((a) => (
                <li key={a.id}>
                  <Link
                    to={`/analyses/${a.id}`}
                    className="flex items-center gap-4 px-4 py-4 transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                  >
                    <Badge tone={toneForScore(a.score)} mono>
                      <span aria-hidden="true">{a.score}</span>
                      <span className="sr-only">
                        Match score {a.score} out of 100
                      </span>
                    </Badge>
                    <span className="min-w-0 flex-1 truncate text-sm font-medium text-ink">
                      {a.jobTitle}
                    </span>
                    <span className="shrink-0 text-xs text-muted">
                      {formatRelativeTime(a.createdAt)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            onPageChange={goToPage}
            isFetching={query.isFetching}
          />
        </>
      )}
    </div>
  );
}
