import { AnalysisIcon, JobIcon, ResumeIcon } from "../layout/icons";
import { cn } from "../../lib/cn";

interface EvidenceThreadProps {
  className?: string;
}

const NODES = [
  { label: "Resume", Icon: ResumeIcon },
  { label: "Job description", Icon: JobIcon },
  { label: "Analysis", Icon: AnalysisIcon },
] as const;

/**
 * The M6 signature motif in miniature: Resume → Job description → Analysis,
 * three nodes joined by a thin cobalt thread. Decorative and used sparingly on
 * the dashboard — the full evidence experience lands in M6.6. Labelled for
 * screen readers as a single descriptive image.
 */
export function EvidenceThread({ className }: EvidenceThreadProps) {
  return (
    <ul
      role="img"
      aria-label="The workflow: resume, then job description, then analysis"
      className={cn("flex flex-wrap items-center gap-y-2 gap-x-2", className)}
    >
      {NODES.map(({ label, Icon }, i) => (
        <li key={label} className="flex items-center gap-2" aria-hidden="true">
          <span className="inline-flex items-center gap-1.5 whitespace-nowrap text-xs font-medium text-muted">
            <span className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-accent-soft text-accent">
              <Icon className="h-3.5 w-3.5" />
            </span>
            {label}
          </span>
          {i < NODES.length - 1 && (
            <span className="h-px w-5 shrink-0 bg-accent/40 sm:w-8" />
          )}
        </li>
      ))}
    </ul>
  );
}
