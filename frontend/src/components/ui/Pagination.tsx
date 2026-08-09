import { Button } from "./Button";

interface PaginationProps {
  /** Zero-based current page index. */
  page: number;
  totalPages: number;
  /** Total item count, used for the "N–M of T" summary. */
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  /** Dim controls while a page fetch is in flight. */
  isFetching?: boolean;
}

/**
 * Prev/next pager driven by Spring `Page` metadata (never `content.length`).
 * Buttons disable at the ends and while fetching; the range summary lives in an
 * `aria-live="polite"` region so it's announced as the page changes.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  isFetching = false,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const firstItem = page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, totalElements);
  const isFirst = page <= 0;
  const isLast = page >= totalPages - 1;

  return (
    <nav
      aria-label="Resume list pages"
      className="mt-6 flex items-center justify-between gap-4"
    >
      <p className="text-sm text-muted" aria-live="polite">
        {firstItem}–{lastItem} of {totalElements}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={isFirst || isFetching}
        >
          Previous
        </Button>
        <span className="text-sm text-muted" aria-hidden="true">
          Page {page + 1} of {totalPages}
        </span>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={isLast || isFetching}
        >
          Next
        </Button>
      </div>
    </nav>
  );
}
