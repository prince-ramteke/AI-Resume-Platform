import { cn } from "../../lib/cn";

interface SkeletonProps {
  className?: string;
}

/**
 * Neutral placeholder block for loading states. Sized entirely by `className`
 * so callers can mirror the final content's dimensions (minimizes layout
 * shift). Decorative — hidden from assistive tech; sections announce their own
 * loading status. The pulse is auto-disabled under prefers-reduced-motion by
 * the global rule in index.css.
 */
export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "animate-pulse rounded-control bg-surface-sunken",
        className
      )}
    />
  );
}
