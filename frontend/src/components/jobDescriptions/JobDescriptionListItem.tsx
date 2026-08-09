import { Button, LinkButton } from "../ui";
import { JobIcon, DownloadIcon, TrashIcon } from "../layout/icons";
import { useDownloadJobDescription } from "../../hooks/useDownloadJobDescription";
import { formatFileSize } from "../../lib/formatFileSize";
import { formatRelativeTime } from "../../lib/formatDate";
import { jobDescriptionKindLabel } from "../../lib/fileKind";
import type { JobDescriptionSummary } from "../../types/jobDescription";

interface JobDescriptionListItemProps {
  jobDescription: JobDescriptionSummary;
  /** Opens the shared delete-confirmation dialog on the list page. */
  onRequestDelete: (jd: JobDescriptionSummary) => void;
}

/**
 * One JD in the list. Metadata on the left, actions on the right. Download owns
 * its own mutation (independent per row) and is only rendered for file-based
 * JDs — text-paste JDs return 404 on `/download` by design.
 */
export function JobDescriptionListItem({
  jobDescription,
  onRequestDelete,
}: JobDescriptionListItemProps) {
  const download = useDownloadJobDescription();
  const kind = jobDescriptionKindLabel(
    jobDescription.contentType,
    jobDescription.title
  );
  const hasFile = jobDescription.contentType !== null;

  return (
    <div className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-start gap-3">
        <span className="mt-0.5 shrink-0 text-accent" aria-hidden="true">
          <JobIcon />
        </span>
        <div className="min-w-0">
          <p
            className="truncate text-sm font-medium text-ink"
            title={jobDescription.title}
          >
            {jobDescription.title}
          </p>
          <p className="mt-0.5 text-xs text-muted">
            {kind}
            {jobDescription.fileSize !== null && (
              <> · {formatFileSize(jobDescription.fileSize)}</>
            )}{" "}
            · {formatRelativeTime(jobDescription.createdAt)}
          </p>
          {download.isError && (
            <p role="alert" className="mt-1 text-xs text-danger">
              Couldn't download. Try again.
            </p>
          )}
        </div>
      </div>

      <div className="flex shrink-0 flex-wrap items-center gap-2">
        <LinkButton
          to={`/job-descriptions/${jobDescription.id}`}
          variant="secondary"
          size="sm"
        >
          View
        </LinkButton>
        {hasFile && (
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<DownloadIcon className="h-4 w-4" />}
            isLoading={download.isPending}
            onClick={() =>
              download.mutate({
                id: jobDescription.id,
                fallbackName: jobDescription.title,
              })
            }
          >
            Download
          </Button>
        )}
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<TrashIcon className="h-4 w-4" />}
          className="text-danger hover:bg-danger-soft"
          onClick={() => onRequestDelete(jobDescription)}
        >
          Delete
        </Button>
      </div>
    </div>
  );
}
