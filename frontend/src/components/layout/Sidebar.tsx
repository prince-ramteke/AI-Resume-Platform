import { Link, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { cn } from "../../lib/cn";
import {
  AnalysisIcon,
  DashboardIcon,
  HistoryIcon,
  JobIcon,
  ResumeIcon,
} from "./icons";

interface NavItem {
  to: string;
  label: string;
  icon: (props: { className?: string }) => ReactNode;
}

const NAV: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: DashboardIcon },
  { to: "/resumes", label: "Resumes", icon: ResumeIcon },
  { to: "/job-descriptions", label: "Job Descriptions", icon: JobIcon },
  { to: "/analyses/new", label: "New Analysis", icon: AnalysisIcon },
  { to: "/analyses", label: "History", icon: HistoryIcon },
];

/**
 * Active-route matching. `/analyses` (History) must not light up on
 * `/analyses/new`, but should stay active on `/analyses/:id` detail pages.
 */
function isActive(pathname: string, to: string): boolean {
  if (to === "/analyses") {
    return (
      pathname === "/analyses" ||
      (pathname.startsWith("/analyses/") && pathname !== "/analyses/new")
    );
  }
  return pathname === to || pathname.startsWith(`${to}/`);
}

export function Wordmark() {
  return (
    <Link
      to="/dashboard"
      className="inline-flex items-center rounded-control text-lg font-semibold tracking-tight text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg"
    >
      Resume<span className="text-accent">●</span>AI
    </Link>
  );
}

interface SidebarProps {
  className?: string;
  /** Called when a nav link is clicked (used to close the mobile drawer). */
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  const { pathname } = useLocation();

  return (
    <aside
      className={cn(
        "flex h-full w-60 flex-col border-r border-border bg-surface",
        className
      )}
    >
      <div className="flex h-16 items-center px-5">
        <Wordmark />
      </div>

      <nav aria-label="Primary" className="flex-1 space-y-1 px-3 py-2">
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = isActive(pathname, to);
          return (
            <Link
              key={to}
              to={to}
              onClick={onNavigate}
              aria-current={active ? "page" : undefined}
              className={cn(
                "group relative flex items-center gap-3 rounded-control px-3 py-2 text-sm transition-colors",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
                active
                  ? "bg-accent-soft font-medium text-ink"
                  : "text-muted hover:bg-surface-sunken hover:text-ink"
              )}
            >
              {active && (
                <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r bg-accent" />
              )}
              <Icon className={cn(active ? "text-accent" : "text-muted")} />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
