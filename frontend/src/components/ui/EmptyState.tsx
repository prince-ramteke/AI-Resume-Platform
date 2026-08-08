import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
  className?: string;
}

/**
 * An empty screen is an invitation to act — every empty state carries a primary
 * action where one makes sense.
 */
export function EmptyState({
  title,
  description,
  action,
  icon,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center rounded-card border border-dashed border-border bg-surface px-6 py-14 text-center",
        className
      )}
    >
      {icon && <div className="mb-4 text-muted">{icon}</div>}
      <h3 className="font-display text-lg text-ink">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm text-muted">{description}</p>
      )}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
