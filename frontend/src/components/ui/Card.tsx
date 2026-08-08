import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

interface CardProps {
  title?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  /** Adds hover affordance for cards that act as links/rows. */
  interactive?: boolean;
}

/**
 * Standard surface: white card + hairline border on the canvas. Elevation is
 * border-first by design — no shadow here (shadows are reserved for overlays).
 */
export function Card({
  title,
  action,
  children,
  className,
  interactive = false,
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-card border border-border bg-surface",
        interactive &&
          "cursor-pointer transition-colors hover:border-muted/40 hover:bg-surface-sunken",
        className
      )}
    >
      {(title || action) && (
        <div className="flex items-center justify-between gap-4 border-b border-border px-6 py-4">
          {typeof title === "string" ? (
            <h3 className="text-base font-semibold text-ink">{title}</h3>
          ) : (
            title
          )}
          {action}
        </div>
      )}
      <div className="px-6 py-5">{children}</div>
    </div>
  );
}
