import { Link } from "react-router-dom";
import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
  /** Optional back-link ({ to, label }) for detail pages — the breadcrumb strategy. */
  back?: { to: string; label: string };
}

export function PageHeader({ title, description, action, back }: PageHeaderProps) {
  return (
    <div className="mb-8">
      {back && (
        <Link
          to={back.to}
          className="mb-2 inline-flex items-center text-sm text-muted transition-colors hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg"
        >
          ← {back.label}
        </Link>
      )}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl leading-tight text-ink">{title}</h1>
          {description && (
            <p className="mt-1.5 max-w-2xl text-sm text-muted">{description}</p>
          )}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
    </div>
  );
}
