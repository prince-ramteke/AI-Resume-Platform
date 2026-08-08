import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Card, Skeleton } from "../ui";

interface ResourceCardProps {
  to: string;
  label: string;
  icon: ReactNode;
  count: number | undefined;
  /** Query is loading (use React Query's `isPending`, stable across retries). */
  isPending: boolean;
  isError: boolean;
  onRetry: () => void;
  /** Singular/plural noun for the count line, e.g. { one: "resume", many: "resumes" }. */
  noun: { one: string; many: string };
}

/**
 * One resource summary tile: icon + label + total count, linking to the
 * resource's section. Loads independently (skeleton → count), and on error
 * degrades to a compact inline retry — a single failure never blanks the
 * dashboard. On error the tile is a non-navigational container so the retry
 * button is not nested inside a link.
 */
export function ResourceCard({
  to,
  label,
  icon,
  count,
  isPending,
  isError,
  onRetry,
  noun,
}: ResourceCardProps) {
  const header = (
    <div className="flex items-center gap-2 text-muted">
      <span className="text-accent">{icon}</span>
      <span className="text-sm font-medium text-ink">{label}</span>
    </div>
  );

  if (isError) {
    return (
      <Card>
        {header}
        <p className="mt-3 text-sm text-muted">Couldn't load the count.</p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-1 rounded-control text-sm font-medium text-accent hover:text-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        >
          Retry
        </button>
      </Card>
    );
  }

  return (
    <Link
      to={to}
      className="rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg"
    >
      <Card interactive className="h-full">
        {header}
        <div className="mt-3 flex items-baseline gap-2">
          {isPending || count === undefined ? (
            <Skeleton className="h-8 w-14" />
          ) : (
            <>
              <span className="font-mono text-3xl leading-none text-ink">
                {count}
              </span>
              <span className="text-sm text-muted">
                {count === 1 ? noun.one : noun.many}
              </span>
            </>
          )}
        </div>
      </Card>
    </Link>
  );
}
