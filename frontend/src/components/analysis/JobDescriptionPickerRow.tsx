import { JobIcon } from "../layout/icons";
import { cn } from "../../lib/cn";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";
import { jobDescriptionKindLabel } from "../../lib/fileKind";
import type { JobDescriptionSummary } from "../../types/jobDescription";

interface JobDescriptionPickerRowProps {
  jobDescription: JobDescriptionSummary;
  selected: boolean;
  disabled?: boolean;
  onSelect: (id: number) => void;
}

/**
 * A single row in the NewAnalysis JD picker — the JD counterpart to
 * ResumePickerRow. Same reasoning: purpose-built to keep the CRUD row's
 * actions out of a selection surface.
 */
export function JobDescriptionPickerRow({
  jobDescription,
  selected,
  disabled,
  onSelect,
}: JobDescriptionPickerRowProps) {
  const kind = jobDescriptionKindLabel(
    jobDescription.contentType,
    jobDescription.title
  );
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={() => onSelect(jobDescription.id)}
      className={cn(
        "flex w-full items-center gap-3 rounded-control border bg-surface px-3 py-3 text-left transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-accent",
        selected
          ? "border-accent ring-1 ring-accent"
          : "border-border hover:bg-surface-sunken",
        disabled && "cursor-not-allowed opacity-60"
      )}
    >
      <span className="shrink-0 text-accent" aria-hidden="true">
        <JobIcon />
      </span>
      <span className="min-w-0 flex-1">
        <span
          className="block truncate text-sm font-medium text-ink"
          title={jobDescription.title}
        >
          {jobDescription.title}
        </span>
        <span className="mt-0.5 block text-xs text-muted">
          {kind}
          {jobDescription.fileSize !== null && (
            <> · {formatFileSize(jobDescription.fileSize)}</>
          )}{" "}
          · {formatRelativeTime(jobDescription.createdAt)}
        </span>
      </span>
      <SelectionDot selected={selected} />
    </button>
  );
}

function SelectionDot({ selected }: { selected: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "shrink-0 h-4 w-4 rounded-full border-2",
        selected ? "border-accent bg-accent" : "border-border bg-surface"
      )}
    />
  );
}
