import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "../../components/layout/PageHeader";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LinkButton,
  Skeleton,
  ConfirmDialog,
} from "../../components/ui";
import {
  JobIcon,
  DownloadIcon,
  UploadIcon,
  TrashIcon,
} from "../../components/layout/icons";
import { JobDescriptionEditDialog } from "../../components/jobDescriptions/JobDescriptionEditDialog";
import { useJobDescription } from "../../hooks/useJobDescription";
import { useUpdateJobDescription } from "../../hooks/useUpdateJobDescription";
import { useDeleteJobDescription } from "../../hooks/useDeleteJobDescription";
import { useDownloadJobDescription } from "../../hooks/useDownloadJobDescription";
import { parseApiError } from "../../api/errors";
import { formatFileSize } from "../../lib/formatFileSize";
import { jobDescriptionKindLabel } from "../../lib/fileKind";

const BACK = { to: "/job-descriptions", label: "Job Descriptions" };

function absoluteDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function DetailSkeleton() {
  return (
    <Card>
      <Skeleton className="h-6 w-64" />
      <Skeleton className="mt-3 h-4 w-40" />
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-4 w-32" />
        ))}
      </div>
    </Card>
  );
}

/**
 * Single JD view: metadata, raw-text preview, and the edit / download / delete
 * actions. A 404 (missing or not owned — enumeration defense) renders a neutral
 * not-found state, never a permissions error. Download only appears for
 * file-based JDs; text-paste JDs return 404 on `/download` by design so we
 * gate the button off entirely rather than firing and catching.
 */
export function JobDescriptionDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const idNum = Number(idParam);
  const validId = Number.isInteger(idNum) && idNum > 0;

  const navigate = useNavigate();
  const query = useJobDescription(idNum);

  const [editOpen, setEditOpen] = useState(false);
  const update = useUpdateJobDescription(idNum);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const del = useDeleteJobDescription();

  const download = useDownloadJobDescription();

  if (!validId) {
    return (
      <div>
        <PageHeader title="Job Description" back={BACK} />
        <EmptyState
          title="Job description not found"
          description="This job description isn't available. It may have been deleted."
          action={
            <LinkButton to="/job-descriptions">
              Back to job descriptions
            </LinkButton>
          }
        />
      </div>
    );
  }

  if (query.isPending) {
    return (
      <div>
        <PageHeader title="Job Description" back={BACK} />
        <DetailSkeleton />
      </div>
    );
  }

  if (query.isError) {
    const parsed = parseApiError(query.error);
    if (parsed.status === 404) {
      return (
        <div>
          <PageHeader title="Job Description" back={BACK} />
          <EmptyState
            title="Job description not found"
            description="This job description isn't available. It may have been deleted."
            action={
              <LinkButton to="/job-descriptions">
                Back to job descriptions
              </LinkButton>
            }
          />
        </div>
      );
    }
    return (
      <div>
        <PageHeader title="Job Description" back={BACK} />
        <ErrorState
          title="Couldn't load this job description"
          message={parsed.message}
          onRetry={() => query.refetch()}
        />
      </div>
    );
  }

  const jd = query.data;
  const hasFile = jd.contentType !== null;
  const kind = jobDescriptionKindLabel(jd.contentType, jd.title);

  function handleUpdate(input: { title: string; rawText: string }) {
    update.mutate(input, {
      onSuccess: () => setEditOpen(false),
    });
  }

  function confirmDelete() {
    del.mutate(idNum, {
      onSuccess: () => navigate("/job-descriptions"),
    });
  }

  return (
    <div>
      <PageHeader title="Job Description" back={BACK} />

      {update.isSuccess && !editOpen && (
        <div className="mb-6">
          <Alert tone="success">Job description updated.</Alert>
        </div>
      )}

      <Card>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 items-start gap-3">
            <span className="mt-0.5 shrink-0 text-accent" aria-hidden="true">
              <JobIcon />
            </span>
            <div className="min-w-0">
              <h2
                className="break-words font-display text-xl text-ink"
                title={jd.title}
              >
                {jd.title}
              </h2>
              <p className="mt-0.5 text-sm text-muted">
                {kind}
                {jd.fileSize !== null && (
                  <> · {formatFileSize(jd.fileSize)}</>
                )}
              </p>
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center gap-2">
            {hasFile && (
              <Button
                variant="secondary"
                size="sm"
                leftIcon={<DownloadIcon className="h-4 w-4" />}
                isLoading={download.isPending}
                onClick={() =>
                  download.mutate({ id: jd.id, fallbackName: jd.title })
                }
              >
                Download
              </Button>
            )}
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<UploadIcon className="h-4 w-4" />}
              onClick={() => {
                update.reset();
                setEditOpen(true);
              }}
            >
              Edit
            </Button>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<TrashIcon className="h-4 w-4" />}
              className="text-danger hover:bg-danger-soft"
              onClick={() => {
                del.reset();
                setDeleteOpen(true);
              }}
            >
              Delete
            </Button>
          </div>
        </div>

        {download.isError && (
          <p role="alert" className="mt-3 text-sm text-danger">
            Couldn't download the file. Try again.
          </p>
        )}

        <dl className="mt-6 grid gap-4 border-t border-border pt-6 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Created
            </dt>
            <dd className="mt-1 text-sm text-ink">
              {absoluteDate(jd.createdAt)}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Last updated
            </dt>
            <dd className="mt-1 text-sm text-ink">
              {jd.updatedAt ? absoluteDate(jd.updatedAt) : "—"}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Pages
            </dt>
            <dd className="mt-1 text-sm text-ink">{jd.pageCount ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-muted">
              Language
            </dt>
            <dd className="mt-1 text-sm text-ink">{jd.language ?? "—"}</dd>
          </div>
        </dl>
      </Card>

      <section aria-labelledby="jd-raw-text-heading" className="mt-8">
        <h2
          id="jd-raw-text-heading"
          className="mb-3 font-display text-lg text-ink"
        >
          Description text
        </h2>
        <Card>
          {jd.rawText?.trim() ? (
            <div className="max-h-96 overflow-y-auto whitespace-pre-wrap break-words text-sm leading-relaxed text-ink">
              {jd.rawText}
            </div>
          ) : (
            <p className="text-sm text-muted">
              No text is stored for this job description.
            </p>
          )}
        </Card>
      </section>

      <JobDescriptionEditDialog
        open={editOpen}
        jobDescription={jd}
        isPending={update.isPending}
        errorMessage={
          update.isError ? parseApiError(update.error).message : null
        }
        onSubmit={handleUpdate}
        onClose={() => {
          if (!update.isPending) setEditOpen(false);
        }}
      />

      <ConfirmDialog
        open={deleteOpen}
        title="Delete job description"
        description={`"${jd.title}" will be removed from your job descriptions. This can't be undone here.`}
        confirmLabel="Delete"
        isLoading={del.isPending}
        errorMessage={del.isError ? parseApiError(del.error).message : null}
        onConfirm={confirmDelete}
        onCancel={() => {
          if (del.isPending) return;
          del.reset();
          setDeleteOpen(false);
        }}
      />
    </div>
  );
}
