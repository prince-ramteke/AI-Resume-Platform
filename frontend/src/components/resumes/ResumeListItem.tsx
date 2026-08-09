import { Button, LinkButton } from "../ui";
import { ResumeIcon, DownloadIcon, TrashIcon } from "../layout/icons";
import { useDownloadResume } from "../../hooks/useDownloadResume";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";
import { fileKindLabel } from "../../lib/fileKind";
import type { ResumeSummary } from "../../types/resume";

interface ResumeListItemProps {
  resume: ResumeSummary;
  /** Opens the shared delete-confirmation dialog on the list page. */
  onRequestDelete: (resume: ResumeSummary) => void;
}

/**
 * One resume in the list. Metadata on the left, actions on the right. Download
 * owns its own mutation (independent per row); View is a link-styled button;
 * Delete defers to the page's single shared confirm dialog.
 */
export function ResumeListItem({ resume, onRequestDelete }: ResumeListItemProps) {
  const download = useDownloadResume();
  const kind = fileKindLabel(resume.contentType, resume.filename);

  return (
    <div className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-start gap-3">
        <span className="mt-0.5 shrink-0 text-accent" aria-hidden="true">
          <ResumeIcon />
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-ink" title={resume.filename}>
            {resume.filename}
          </p>
          <p className="mt-0.5 text-xs text-muted">
            {kind} · {formatFileSize(resume.fileSize)} ·{" "}
            {formatRelativeTime(resume.createdAt)}
          </p>
          {download.isError && (
            <p role="alert" className="mt-1 text-xs text-danger">
              Couldn't download. Try again.
            </p>
          )}
        </div>
      </div>

      <div className="flex shrink-0 flex-wrap items-center gap-2">
        <LinkButton to={`/resumes/${resume.id}`} variant="secondary" size="sm">
          View
        </LinkButton>
        <Button
          variant="secondary"
          size="sm"
          leftIcon={<DownloadIcon className="h-4 w-4" />}
          isLoading={download.isPending}
          onClick={() =>
            download.mutate({ id: resume.id, fallbackName: resume.filename })
          }
        >
          Download
        </Button>
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<TrashIcon className="h-4 w-4" />}
          className="text-danger hover:bg-danger-soft"
          onClick={() => onRequestDelete(resume)}
        >
          Delete
        </Button>
      </div>
    </div>
  );
}
