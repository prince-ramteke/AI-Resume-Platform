import { ResumeIcon } from "../layout/icons";
import { cn } from "../../lib/cn";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";
import { fileKindLabel } from "../../lib/fileKind";
import type { ResumeSummary } from "../../types/resume";

interface ResumePickerRowProps {
  resume: ResumeSummary;
  selected: boolean;
  disabled?: boolean;
  onSelect: (id: number) => void;
}

/**
 * A single row in the NewAnalysis resume picker. Deliberately a purpose-built
 * component, not a variant of `ResumeListItem` — the CRUD row has Delete /
 * Download / View actions that don't belong in a selection UI. `role="radio"`
 * makes the whole row keyboard-selectable via the parent's `role="radiogroup"`.
 */
export function ResumePickerRow({
  resume,
  selected,
  disabled,
  onSelect,
}: ResumePickerRowProps) {
  const kind = fileKindLabel(resume.contentType, resume.filename);
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={() => onSelect(resume.id)}
      className={cn(
        "flex w-full items-center gap-3 rounded-control border bg-surface px-3 py-3 text-left transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-accent",
        selected
          ? "border-accent ring-1 ring-accent"
          : "border-border hover:bg-surface-sunken",
        disabled && "cursor-not-allowed opacity-60"
      )}
    >
      <span className="shrink-0 text-accent" aria-hidden="true">
        <ResumeIcon />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-medium text-ink" title={resume.filename}>
          {resume.filename}
        </span>
        <span className="mt-0.5 block text-xs text-muted">
          {kind} · {formatFileSize(resume.fileSize)} ·{" "}
          {formatRelativeTime(resume.createdAt)}
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
